```
>> OPEN the connection to the database…
>> BEGIN;
>> DELETE FROM <table> WHERE id = `some-id`
>> COMMIT; // or ROLLBACK;

>> CLOSE the connection.