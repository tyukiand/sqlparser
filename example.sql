USE myDatabase;
SELECT id, name, code FROM snippets WHERE is_java IS NOT NULL ORDER BY creation_date;
INSERT INTO snippets (id, name, code, creation_date) VALUES (42, 58, "print\"hello, world\"", NOW());
DELETE FROM db.snippets WHERE id > 12345;
