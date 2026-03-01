-- Backfill missing patient phone values to satisfy NOT NULL + format checks.
-- Generates a deterministic 10-digit placeholder: 9 + zero-padded patient id.
UPDATE patient
SET phone = CONCAT('9', LPAD(id, 9, '0'))
WHERE phone IS NULL OR TRIM(phone) = '';

-- Validation checks
SELECT COUNT(*) AS null_or_blank_phone
FROM patient
WHERE phone IS NULL OR TRIM(phone) = '';

SELECT COUNT(*) AS invalid_phone
FROM patient
WHERE phone IS NOT NULL
  AND TRIM(phone) <> ''
  AND phone NOT REGEXP '^\\+?[0-9]{10,15}$';
