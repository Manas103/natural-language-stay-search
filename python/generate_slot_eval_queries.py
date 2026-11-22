"""Expands the 30 hand-written, hand-labeled templates in `slot_eval_templates.py` against 10
real, distinct grounding listings into 300 per-slot query-understanding evaluation rows: for each
row, the query text and the single correct extraction this project's hand-assigned judgment says
`QueryParser` should produce for price, bedroom, amenity and neighbourhood.

Usage:
    python generate_slot_eval_queries.py <listings_csv> <output_csv> [--seed 29]

<listings_csv> is normally `src/test/resources/listings_sample.csv` (the committed 200-row real
sample); this evaluation is about text-parsing behavior, not corpus scale, so the small committed
sample is enough real grounding data and needs no download.
"""
import csv
import random
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from slot_eval_templates import all_templates, BEDROOM_WORDS  # noqa: E402
from seed_queries import AMENITY_KEYWORDS  # noqa: E402

FIELDS = [
    "query_id", "template_id", "slot_type", "query_text",
    "expected_price_hit", "expected_price_cents",
    "expected_bedroom_hit", "expected_bedrooms",
    "expected_amenity_hit", "expected_amenity_keywords",
    "expected_neighbourhood_hit", "expected_neighbourhood",
]


def pick_grounding_listings(listings_csv_path, count, rng):
    with open(listings_csv_path, encoding="utf-8", newline="") as f:
        rows = list(csv.DictReader(f))
    usable = [
        r for r in rows
        if r.get("neighbourhood_cleansed") and r.get("price") and r.get("bedrooms")
        and r["price"].strip().lstrip("$").replace(",", "").replace(".00", "").isdigit()
        and r["bedrooms"].strip().isdigit() and 1 <= int(r["bedrooms"]) <= 5
    ]
    rng.shuffle(usable)
    seen_neighbourhoods = set()
    chosen = []
    for row in usable:
        n = row["neighbourhood_cleansed"]
        if n in seen_neighbourhoods:
            continue
        seen_neighbourhoods.add(n)
        chosen.append(row)
        if len(chosen) == count:
            break
    if len(chosen) < count:
        raise RuntimeError(f"only found {len(chosen)} distinct-neighbourhood groundable listings, need {count}")
    return chosen


def price_dollars_from(row):
    raw = row["price"].strip().lstrip("$").replace(",", "")
    return max(50, int(float(raw)))


def empty_expected():
    return {
        "expected_price_hit": False, "expected_price_cents": "",
        "expected_bedroom_hit": False, "expected_bedrooms": "",
        "expected_amenity_hit": False, "expected_amenity_keywords": "",
        "expected_neighbourhood_hit": False, "expected_neighbourhood": "",
    }


def build_row(template, grounding, query_id):
    expected = empty_expected()
    price_dollars = price_dollars_from(grounding)
    bedrooms = int(grounding["bedrooms"])
    neighbourhood = grounding["neighbourhood_cleansed"]

    if template["slot"] == "price":
        text = template["shape"].format(price=price_dollars)
        if template["hit"]:
            expected["expected_price_hit"] = True
            expected["expected_price_cents"] = price_dollars * 100
    elif template["slot"] == "bedroom":
        text = template["shape"].format(bedroom=bedrooms, bedroom_word=BEDROOM_WORDS[bedrooms])
        if template["hit"]:
            expected["expected_bedroom_hit"] = True
            expected["expected_bedrooms"] = bedrooms
    elif template["slot"] == "amenity":
        text = template["shape"]
        if template["hit"]:
            expected["expected_amenity_hit"] = True
            expected["expected_amenity_keywords"] = ";".join(AMENITY_KEYWORDS_FOR(template))
    elif template["slot"] == "neighbourhood":
        text = template["shape"].format(neighbourhood=neighbourhood)
        if template["hit"]:
            expected["expected_neighbourhood_hit"] = True
            expected["expected_neighbourhood"] = template.get("fixed_value", neighbourhood.lower())
    else:  # "none"
        text = template["shape"]

    return {
        "query_id": query_id,
        "template_id": template["id"],
        "slot_type": template["slot"],
        "query_text": text,
        **expected,
    }


def AMENITY_KEYWORDS_FOR(template):
    phrase = template["matched_phrase"]
    return AMENITY_KEYWORDS[phrase]


def main():
    if len(sys.argv) < 3:
        print(__doc__)
        sys.exit(1)
    listings_csv_path = sys.argv[1]
    out_csv_path = Path(sys.argv[2])
    seed = 29
    if "--seed" in sys.argv:
        seed = int(sys.argv[sys.argv.index("--seed") + 1])

    rng = random.Random(seed)
    groundings = pick_grounding_listings(listings_csv_path, 10, rng)
    templates = all_templates()
    assert len(templates) == 30, f"expected 30 hand-written templates, got {len(templates)}"

    rows = []
    qid = 1
    for grounding in groundings:
        for template in templates:
            rows.append(build_row(template, grounding, qid))
            qid += 1

    assert len(rows) == 300, f"expected 300 expanded rows, got {len(rows)}"

    out_csv_path.parent.mkdir(parents=True, exist_ok=True)
    with open(out_csv_path, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=FIELDS)
        w.writeheader()
        w.writerows(rows)

    print(f"wrote {len(rows)} hand-labeled slot-eval queries to {out_csv_path}")
    print(f"grounding listings: {len(groundings)} distinct neighbourhoods, templates: {len(templates)}")


if __name__ == "__main__":
    main()
