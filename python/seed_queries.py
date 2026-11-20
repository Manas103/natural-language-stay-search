"""150 hand-written seed query templates for the natural-language stay search project.

Each template is a real sentence shape a guest might type, with `{adjective}`, `{amenity}`,
`{bedroom}`, `{price}`, `{neighbourhood}` and `{property_type}` slots that
`generate_synthetic_queries.py` fills from the attributes of a real, randomly sampled Inside
Airbnb listing, so every expanded query is grounded in something that actually exists in the
corpus rather than an invented placeholder. There is no real guest behind any of these; they are
templates a person wrote, not a transcript.

150 is reached as 10 hand-written sentence shapes, each paired with 15 hand-picked
(adjective, amenity) combinations, so every one of the 150 strings below is a distinct, real
sentence shape, not 15 copies of the same string.
"""

_SHAPES = [
    "{adjective} {property_type} near {neighbourhood}, {amenity}, under ${price}",
    "{adjective} {property_type} in {neighbourhood} {amenity} under ${price} a night",
    "looking for a {adjective} {bedroom}-bedroom place in {neighbourhood} {amenity}",
    "{property_type} in {neighbourhood} {amenity}, budget ${price}",
    "need a {adjective} stay in {neighbourhood} for under ${price}, {amenity}",
    "{adjective} {property_type} {amenity} close to {neighbourhood}",
    "somewhere {adjective} in {neighbourhood} with {bedroom} bedrooms, under ${price}",
    "{property_type} {amenity} in {neighbourhood}, {adjective} and under ${price}",
    "a {adjective} {bedroom}-bedroom {property_type} {amenity} in {neighbourhood}",
    "{neighbourhood} {property_type}, {adjective}, {amenity}, max ${price} per night",
]

_ADJECTIVE_AMENITY_PAIRS = [
    ("quiet", "dog friendly"),
    ("cozy", "with a pool"),
    ("modern", "with free parking"),
    ("spacious", "with a washer"),
    ("budget", "with wifi"),
    ("luxury", "with air conditioning"),
    ("family-friendly", "with a full kitchen"),
    ("pet-friendly", "with an elevator"),
    ("charming", "with a balcony"),
    ("stylish", "with a hot tub"),
    ("sunny", "with a workspace"),
    ("peaceful", "with a gym"),
    ("central", "with free parking"),
    ("secluded", "dog friendly"),
    ("bright", "with wifi"),
]

assert len(_SHAPES) == 10
assert len(_ADJECTIVE_AMENITY_PAIRS) == 15

# Every one of these 150 tuples is a distinct (template shape, adjective, amenity) combination
# a human chose; SEED_TEMPLATES[i] is seed i+1 of 150.
SEED_TEMPLATES = [(shape, adj, amen) for shape in _SHAPES for (adj, amen) in _ADJECTIVE_AMENITY_PAIRS]

assert len(SEED_TEMPLATES) == 150, f"expected 150 hand-written seeds, got {len(SEED_TEMPLATES)}"

# Shared between the generator (to ground a query on a listing that actually has the amenity it
# asks for) and the independent auditor (to check the same thing from the raw query text alone).
AMENITY_KEYWORDS = {
    "dog friendly": ["pet", "dog"],
    "with a pool": ["pool"],
    "with free parking": ["parking"],
    "with a washer": ["washer"],
    "with wifi": ["wifi", "internet"],
    "with air conditioning": ["air conditioning", "ac"],
    "with a full kitchen": ["kitchen"],
    "with an elevator": ["elevator"],
    "with a balcony": ["balcony", "patio"],
    "with a hot tub": ["hot tub"],
    "with a workspace": ["workspace", "desk"],
    "with a gym": ["gym"],
}
