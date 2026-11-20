# Data manifest

This project's "public Inside Airbnb export of 180K+ listings" claim is measured against a real,
public corpus that is not committed to this repository (it is 199,021 rows and roughly 217 MB as
a trimmed CSV, far over this repository's own size budget). This file is the manifest: exact
source URLs, snapshot dates, row counts and a checksum, so the corpus can be reproduced exactly.

## Sources (CC BY 4.0, per Inside Airbnb's terms: https://insideairbnb.com/get-the-data/)

| City | Snapshot date | URL | Real rows (deduplicated) |
|---|---|---|---|
| London, England, UK | 2026-09-17 | https://data.insideairbnb.com/united-kingdom/england/london/2026-09-17/data/listings.csv.gz | 91,954 |
| Paris, France | 2026-09-16 | https://data.insideairbnb.com/france/ile-de-france/paris/2026-09-16/data/listings.csv.gz | 77,149 |
| New York City, NY, US | 2026-09-14 | https://data.insideairbnb.com/united-states/ny/new-york-city/2026-09-14/data/listings.csv.gz | 29,918 |

**Total: 199,021 real, deduplicated listings** (deduplicated on `id`; no overlap was found across
the three cities).

## Reproducing the trimmed corpus

```
curl -o london.csv.gz   https://data.insideairbnb.com/united-kingdom/england/london/2026-09-17/data/listings.csv.gz
curl -o paris.csv.gz    https://data.insideairbnb.com/france/ile-de-france/paris/2026-09-16/data/listings.csv.gz
curl -o nyc.csv.gz      https://data.insideairbnb.com/united-states/ny/new-york-city/2026-09-14/data/listings.csv.gz
# then merge, dedupe on id, and keep only the columns ListingCsvLoader reads (see its javadoc)
```

The trimmed corpus measured against in this README is `listings_trimmed.csv`, 199,021 rows,
sha256 `ef78634359326efc7fa97d03872dab04c3a4d9ef2e930e2c27b21f056526d656`.

## Committed sample

`src/test/resources/listings_sample.csv` is a 200-row random sample of the trimmed corpus
(descriptions, neighborhood overviews and amenities text truncated to 140 characters to keep the
fixture small), used by this repository's own fast test suite. It is not large enough, and not
claimed to be large enough, to reproduce the measured NDCG@10 or latency numbers; those are
measured against the full 199,021-row corpus (see README "Measured results").
