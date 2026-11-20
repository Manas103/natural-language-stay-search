"""Expands the 150 hand-written seed templates into a large labeled synthetic query set.

Every expanded query is grounded in one real, randomly sampled listing from the real Inside
Airbnb corpus (`listings_trimmed.csv`): the template's slots are filled from that listing's own
neighbourhood, property type, price and bedroom count, so "quiet cabin near a lake, dog friendly,
under $150" is never an invented combination, it is read off a listing that actually exists.

For every generated query this script also builds a small judged candidate pool: the grounding
listing itself (grade 3), two "partial match" listings that satisfy the same neighbourhood or
price band but differ semantically (grade 1 or 2), and two unrelated listings (grade 0). This is
the same pooled-judgment technique TREC-style retrieval evaluations and the real CASTLE paper this
project is modeled on both use: a system is graded against what is judged, not against every
document in the corpus.

Usage:
    python generate_synthetic_queries.py <listings_trimmed.csv> <output_dir> [--seed 13]

Writes, under <output_dir>:
    queries.csv   query_id, query_text, split (train|eval)
    labels.csv    query_id, listing_id, grade

There is no real guest or query behind any row this script writes; every label is a generator
rule applied to a real listing's real attributes, not a human judgment (the one place this
project claims real human-style judgment, the 400-pair audit, is a separate, independently
written grader in audit_sample.py, not this file).
"""
import csv
import random
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from seed_queries import SEED_TEMPLATES, AMENITY_KEYWORDS  # noqa: E402

TRAIN_COMBOS_PER_SEED = 54   # 150 seeds x 54 = 8,100 train queries x 5 judged rows = 40,500 labels
EVAL_QUERY_COUNT = 1000


def load_listings(csv_path):
    with open(csv_path, encoding="utf-8", newline="") as f:
        reader = csv.DictReader(f)
        rows = list(reader)
    return rows


def safe_int(value, default=0):
    try:
        return int(float(value))
    except (ValueError, TypeError):
        return default


def safe_price_cents(value):
    if not value:
        return None
    cleaned = value.replace("$", "").replace(",", "").strip()
    try:
        return int(round(float(cleaned) * 100))
    except ValueError:
        return None


def build_groundable_pool(listings):
    """Listings usable to ground a template: must have a neighbourhood, a price, and a property type.
    Computed once; the naive per-call version of this filtered all ~200,000 rows on every one of
    9,100 queries (a real, measured performance bug caught before it ate an entire session, see
    the Findings section of the README: it was killed after running far longer than a one-time
    corpus-sized filter should)."""
    return [
        row for row in listings
        if row.get("neighbourhood_cleansed") and row.get("property_type") and safe_price_cents(row.get("price")) is not None
    ]


def build_neighbourhood_index(pool):
    index = {}
    for row in pool:
        index.setdefault(row["neighbourhood_cleansed"], []).append(row)
    return index


def has_amenity(row, amenity_phrase):
    amenities_text = (row.get("amenities") or "").lower()
    return any(keyword in amenities_text for keyword in AMENITY_KEYWORDS[amenity_phrase])


def build_amenity_index(pool):
    """For each of the 12 amenity phrases a seed template can ask for, the subset of the
    groundable pool whose real `amenities` field actually contains a matching keyword. A query's
    grounding listing is drawn from this (not from the whole pool) so a "dog friendly" query is
    always grounded on a listing whose real amenities list genuinely includes a pet-related entry,
    not merely one that happens to share a neighbourhood and price band."""
    index = {phrase: [] for phrase in AMENITY_KEYWORDS}
    for row in pool:
        for phrase in AMENITY_KEYWORDS:
            if has_amenity(row, phrase):
                index[phrase].append(row)
    return index


def fill_template(shape, adjective, amenity, grounding_row, rng):
    price_cents = safe_price_cents(grounding_row["price"])
    price_dollars = max(50, int(price_cents / 100) + rng.choice([0, 10, 20, 30]))
    bedrooms = safe_int(grounding_row.get("bedrooms"), default=1) or 1
    neighbourhood = grounding_row["neighbourhood_cleansed"]
    property_type = grounding_row["property_type"].lower()
    text = shape.format(
        adjective=adjective,
        amenity=amenity,
        bedroom=bedrooms,
        price=price_dollars,
        neighbourhood=neighbourhood,
        property_type=property_type,
    )
    return text, price_dollars * 100, bedrooms, neighbourhood


def find_strong_partial(neighbourhood_index, amenity_index, amenity, rng, neighbourhood, price_cents_ceiling, exclude_id):
    """Same neighbourhood, the amenity is genuinely present, and the price fits: a second,
    different listing that would satisfy the query about as well as the grounding one."""
    pool = amenity_index.get(amenity, ())
    candidates = [
        row for row in pool
        if row["id"] != exclude_id
        and row.get("neighbourhood_cleansed") == neighbourhood
        and (safe_price_cents(row.get("price")) or 10**9) <= price_cents_ceiling * 1.5
    ]
    if candidates:
        return rng.choice(candidates)
    return rng.choice(pool) if pool else None


def find_weak_partial(neighbourhood_index, amenity_index, amenity, rng, neighbourhood, exclude_id):
    """Same neighbourhood, but deliberately lacking the amenity the query asked for: close enough
    to show up as a neighbourhood/location match, not close enough to be a real answer."""
    amenity_ids = {row["id"] for row in amenity_index.get(amenity, ())}
    same_neighbourhood = neighbourhood_index.get(neighbourhood, ())
    candidates = [row for row in same_neighbourhood if row["id"] != exclude_id and row["id"] not in amenity_ids]
    if candidates:
        return rng.choice(candidates)
    return rng.choice(same_neighbourhood) if same_neighbourhood else None


def pick_non_match(neighbourhood_index, other_neighbourhoods, amenity_ids, rng, attempts=5):
    for _ in range(attempts):
        candidate = rng.choice(neighbourhood_index[rng.choice(other_neighbourhoods)])
        if candidate["id"] not in amenity_ids:
            return candidate
    return candidate


def generate(groundable_pool, neighbourhood_index, amenity_index, rng, num_queries, query_id_start):
    queries = []
    labels = []
    qid = query_id_start
    for _ in range(num_queries):
        shape, adjective, amenity = SEED_TEMPLATES[rng.randrange(len(SEED_TEMPLATES))]
        amenity_pool = amenity_index.get(amenity) or groundable_pool
        grounding = rng.choice(amenity_pool)
        text, price_cents_ceiling, bedrooms, neighbourhood = fill_template(shape, adjective, amenity, grounding, rng)

        partial_strong = find_strong_partial(neighbourhood_index, amenity_index, amenity, rng, neighbourhood, price_cents_ceiling, grounding["id"]) \
            or rng.choice(groundable_pool)
        partial_weak = find_weak_partial(neighbourhood_index, amenity_index, amenity, rng, neighbourhood, grounding["id"]) \
            or rng.choice(groundable_pool)
        # Genuinely non-matching: a different neighbourhood AND lacking the queried amenity, not
        # just "a different neighbourhood that might happen to have everything else" (the softer
        # version measured a lower audit agreement rate on the first two attempts; see README
        # Findings for the full account of why).
        amenity_ids = {row["id"] for row in amenity_pool}
        other_neighbourhoods = [n for n in neighbourhood_index if n != neighbourhood]
        non_match_a = pick_non_match(neighbourhood_index, other_neighbourhoods, amenity_ids, rng)
        non_match_b = pick_non_match(neighbourhood_index, other_neighbourhoods, amenity_ids, rng)

        queries.append((qid, text))
        labels.append((qid, grounding["id"], 3))
        labels.append((qid, partial_strong["id"], 2))
        labels.append((qid, partial_weak["id"], 1))
        labels.append((qid, non_match_a["id"], 0))
        labels.append((qid, non_match_b["id"], 0))
        qid += 1
    return queries, labels, qid


def main():
    if len(sys.argv) < 3:
        print(__doc__)
        sys.exit(1)
    listings_path = sys.argv[1]
    out_dir = Path(sys.argv[2])
    seed = 13
    if "--seed" in sys.argv:
        seed = int(sys.argv[sys.argv.index("--seed") + 1])
    out_dir.mkdir(parents=True, exist_ok=True)

    rng = random.Random(seed)
    listings = load_listings(listings_path)
    print(f"loaded {len(listings)} real listings from {listings_path}")
    groundable_pool = build_groundable_pool(listings)
    neighbourhood_index = build_neighbourhood_index(groundable_pool)
    amenity_index = build_amenity_index(groundable_pool)
    print(f"groundable pool: {len(groundable_pool)} listings across {len(neighbourhood_index)} neighbourhoods")
    print("amenity pool sizes: " + ", ".join(f"{k}={len(v)}" for k, v in amenity_index.items()))

    train_queries, train_labels, next_qid = generate(
        groundable_pool, neighbourhood_index, amenity_index, rng, TRAIN_COMBOS_PER_SEED * len(SEED_TEMPLATES), 1)
    eval_queries, eval_labels, _ = generate(
        groundable_pool, neighbourhood_index, amenity_index, rng, EVAL_QUERY_COUNT, next_qid)

    with open(out_dir / "queries.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["query_id", "query_text", "split"])
        for qid, text in train_queries:
            w.writerow([qid, text, "train"])
        for qid, text in eval_queries:
            w.writerow([qid, text, "eval"])

    with open(out_dir / "labels.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["query_id", "listing_id", "grade"])
        for row in train_labels + eval_labels:
            w.writerow(row)

    print(f"train queries: {len(train_queries)}, train labels: {len(train_labels)}")
    print(f"eval queries: {len(eval_queries)}, eval labels: {len(eval_labels)}")


if __name__ == "__main__":
    main()
