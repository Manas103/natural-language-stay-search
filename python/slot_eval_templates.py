"""30 hand-written query templates for the per-slot query-understanding evaluation, each one
labeled by hand with the slot value a human reading the sentence would say the guest actually
wants, regardless of whether `QueryParser`'s current rules happen to catch that phrasing. 10 real,
distinct grounding listings from the committed 200-row sample fill in the real price, bedroom
count and neighbourhood each template needs, so the 30 x 10 = 300 expanded queries are grounded in
real data the same way the rest of this project's query sets are, while the hand-assigned
ground-truth judgment per template is fixed.

This is the deliberate design choice that keeps the evaluation honest rather than circular: the
label is "what does this sentence mean", never "what will the regex do". Half of the bedroom and
amenity templates, three of the eight price templates and one neighbourhood template use a
phrasing (a word-form number, an abbreviation, a synonym outside `AmenityKeywords`'s 12 phrases, a
shortened real place name) this project's dictionary-and-regex parser is expected to miss; scoring
those as misses, not as "nothing to extract", is what makes the measured recall below 100% on
three of the four slots, which is the honest result.

Each template targets exactly one slot (price, bedroom, amenity or neighbourhood) or none, and is
written so it cannot accidentally trigger a different slot (no stray "$" in a bedroom template, no
stray digit-dash-word in a price template). This keeps the per-slot confusion matrix clean: a
template's non-target slots are always a true "nothing to extract" case.
"""

BEDROOM_WORDS = {1: "one", 2: "two", 3: "three", 4: "four", 5: "five"}


def _price_templates():
    return [
        {"id": 1, "slot": "price", "shape": "a place under ${price}", "hit": True},
        {"id": 2, "slot": "price", "shape": "budget of ${price} or less", "hit": True},
        {"id": 3, "slot": "price", "shape": "around {price} dollars a night", "hit": True},
        {"id": 4, "slot": "price", "shape": "up to ${price} per night", "hit": True},
        {"id": 5, "slot": "price", "shape": "{price} dollars max", "hit": True},
        {"id": 6, "slot": "price", "shape": "no more than ${price}", "hit": True},
        {"id": 7, "slot": "price", "shape": "willing to pay about {price} a night", "hit": True},
        {"id": 8, "slot": "price", "shape": "${price} is my limit", "hit": True},
    ]


def _bedroom_templates():
    return [
        {"id": 9, "slot": "bedroom", "shape": "a {bedroom}-bedroom apartment", "hit": True},
        {"id": 10, "slot": "bedroom", "shape": "a {bedroom} bedroom house", "hit": True},
        {"id": 11, "slot": "bedroom", "shape": "needs {bedroom_word} bedrooms", "hit": True},
        {"id": 12, "slot": "bedroom", "shape": "a {bedroom}BR unit", "hit": True},
        {"id": 13, "slot": "bedroom", "shape": "{bedroom} bed place, no more", "hit": True},
        {"id": 14, "slot": "bedroom", "shape": "sleeps enough for {bedroom} bedrooms", "hit": True},
        {"id": 15, "slot": "bedroom", "shape": "a {bedroom}-bed-room flat", "hit": True},
        {"id": 16, "slot": "bedroom", "shape": "{bedroom} bedrooms minimum please", "hit": True},
    ]


# query text, the AmenityKeywords.java phrase the guest's intent actually maps to (used to look
# up the expected keyword list, regardless of whether the query text itself uses that exact
# phrase), whether the query's own wording is the exact recognized phrase.
_AMENITY_CASES = [
    ("dog friendly place to stay", "dog friendly", True),
    ("place with a pool out back", "with a pool", True),
    ("has a swimming pool on site", "with a pool", False),
    ("free parking available nearby", "with free parking", False),
    ("with free parking included", "with free parking", True),
    ("pet accommodating landlord", "dog friendly", False),
    ("with wifi throughout", "with wifi", True),
    ("has fast internet in every room", "with wifi", False),
]


def _amenity_templates():
    templates = []
    for i, (shape, intended_phrase, exact_phrase_used) in enumerate(_AMENITY_CASES, start=17):
        templates.append({
            "id": i, "slot": "amenity", "shape": shape, "hit": True,
            "matched_phrase": intended_phrase, "exact_phrase_used": exact_phrase_used,
        })
    return templates


def _neighbourhood_templates():
    return [
        {"id": 25, "slot": "neighbourhood", "shape": "near {neighbourhood}", "hit": True},
        {"id": 26, "slot": "neighbourhood", "shape": "a stay in {neighbourhood}", "hit": True},
        # A real-sounding but invented place name, naming it deliberately: the guest clearly
        # wants a specific neighbourhood, but this project's NeighbourhoodMatcher only ever
        # recognizes a neighbourhood string the loaded corpus itself contains, so no name outside
        # that set, real or not, can match; "fixed_value" below overrides the grounding listing's
        # own (irrelevant here) neighbourhood.
        {"id": 27, "slot": "neighbourhood", "shape": "somewhere close to Elmridge Hollow", "hit": True,
         "fixed_value": "elmridge hollow"},
        {"id": 28, "slot": "none", "shape": "a place to relax for a few days", "hit": False},
        {"id": 29, "slot": "neighbourhood", "shape": "{neighbourhood} apartment please", "hit": True},
        {"id": 30, "slot": "neighbourhood", "shape": "visiting the {neighbourhood} area", "hit": True},
    ]


def all_templates():
    return _price_templates() + _bedroom_templates() + _amenity_templates() + _neighbourhood_templates()
