"""Audits 400 of the synthetic (query, listing, grade) labels against an independently written
grader, and reports the agreement rate. This is the one place in this project that stands in for
real human judgment: there is no human rater available in a headless build, so the "second
opinion" is a second, deliberately different heuristic (keyword and price matching against the
raw query text) rather than a copy of generate_synthetic_queries.py's own grounding-based logic.
Disagreements are written out as the project's regression suite: if a future change to the
generator or the grader starts agreeing (or disagreeing) with these specific pairs differently,
that is a real, inspectable behavior change.

Usage:
    python audit_sample.py <listings_trimmed.csv> <data_dir> [--seed 7] [--sample-size 400]

Reads <data_dir>/queries.csv and <data_dir>/labels.csv (written by generate_synthetic_queries.py).
Writes <data_dir>/audit_result.csv (every audited pair, both grades, agree/disagree) and
<data_dir>/audit_disagreements.csv (the regression suite).
"""
import csv
import random
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from seed_queries import AMENITY_KEYWORDS  # noqa: E402


def load_csv(path):
    with open(path, encoding="utf-8", newline="") as f:
        return list(csv.DictReader(f))


def load_listings_by_id(path):
    by_id = {}
    with open(path, encoding="utf-8", newline="") as f:
        for row in csv.DictReader(f):
            by_id[row["id"]] = row
    return by_id


PRICE_RE = re.compile(r"\$(\d+)")


def independent_grade(query_text, listing_row):
    """A deliberately different heuristic from the generator's own grounding logic: pure keyword
    and price matching against the raw query text and the listing's real fields, with no
    knowledge of which listing the query was originally grounded on."""
    score = 0
    neighbourhood = (listing_row.get("neighbourhood_cleansed") or "").lower()
    if neighbourhood and neighbourhood in query_text.lower():
        score += 1

    price_match = PRICE_RE.search(query_text)
    if price_match:
        ceiling_cents = int(price_match.group(1)) * 100
        try:
            listing_price_cents = int(round(float(listing_row.get("price", "0").replace("$", "").replace(",", "") or 0) * 100))
        except ValueError:
            listing_price_cents = None
        if listing_price_cents is not None and listing_price_cents <= ceiling_cents:
            score += 1

    amenities_text = (listing_row.get("amenities") or "").lower()
    query_lower = query_text.lower()
    matched_amenity = False
    for phrase, keywords in AMENITY_KEYWORDS.items():
        if phrase in query_lower and any(k in amenities_text for k in keywords):
            matched_amenity = True
            break
    if matched_amenity:
        score += 1

    return score


def main():
    if len(sys.argv) < 3:
        print(__doc__)
        sys.exit(1)
    listings_path = sys.argv[1]
    data_dir = Path(sys.argv[2])
    seed = 7
    sample_size = 400
    if "--seed" in sys.argv:
        seed = int(sys.argv[sys.argv.index("--seed") + 1])
    if "--sample-size" in sys.argv:
        sample_size = int(sys.argv[sys.argv.index("--sample-size") + 1])

    rng = random.Random(seed)
    listings_by_id = load_listings_by_id(listings_path)
    queries = {row["query_id"]: row["query_text"] for row in load_csv(data_dir / "queries.csv")}
    labels = load_csv(data_dir / "labels.csv")

    sample = rng.sample(labels, sample_size)

    results = []
    agree_count = 0
    for row in sample:
        qid, listing_id, grade = row["query_id"], row["listing_id"], int(row["grade"])
        query_text = queries[qid]
        listing_row = listings_by_id.get(listing_id)
        if listing_row is None:
            continue
        audit_grade = independent_grade(query_text, listing_row)
        original_relevant = grade >= 2
        audit_relevant = audit_grade >= 2
        agree = original_relevant == audit_relevant
        if agree:
            agree_count += 1
        results.append((qid, listing_id, query_text, grade, audit_grade, original_relevant, audit_relevant, agree))

    with open(data_dir / "audit_result.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["query_id", "listing_id", "query_text", "generator_grade", "audit_grade", "generator_relevant", "audit_relevant", "agree"])
        w.writerows(results)

    disagreements = [r for r in results if not r[7]]
    with open(data_dir / "audit_disagreements.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["query_id", "listing_id", "query_text", "generator_grade", "audit_grade", "generator_relevant", "audit_relevant"])
        w.writerows(r[:7] for r in disagreements)

    agreement_rate = agree_count / len(results) if results else 0.0
    print(f"audited {len(results)} pairs")
    print(f"agreement_rate={agreement_rate:.4f}")
    print(f"disagreements={len(disagreements)}")


if __name__ == "__main__":
    main()
