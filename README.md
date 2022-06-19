# Flink JDBC Source Exactly-Once Example

A sample implementation as the title.
Flink does not natively provide such a source/connector.

## To Run Pipeline:
- run `gradlew h2`
- load `db/schema.sql`
- run `TestTaskApplication` with `h2` profile

Mock events will automatically be inserted by `MockDbFeedsApp`

`timestamp` on events are skewed by introducing random variants.

## Author
Cause Chung (cuzfrog@gmail.com)