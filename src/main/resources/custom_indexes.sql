--------------------------------------
--------------------------------------
--  Custom Indexes  for MIMIC4FHIR  --
--------------------------------------
--------------------------------------


----------
-- core --
----------

SET search_path TO mimic_icu;
CREATE INDEX chartevents_idx04 ON mimic_icu.chartevents (hadm_id);

----------
-- hosp --
----------

SET search_path TO mimic_hosp;
CREATE INDEX prescriptions_idx02 ON mimic_hosp.prescriptions (subject_id,hadm_id);
CREATE INDEX labevent_idx03 ON mimic_hosp.labevents (subject_id,hadm_id);
