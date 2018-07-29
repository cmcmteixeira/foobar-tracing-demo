CREATE TYPE RequestedDrinkStatus AS ENUM ('FAILED', 'SUCCESSFUL', 'PROCESSING');

CREATE TABLE requested_drink (
 id SERIAL,
 drink varchar(25),
 identifier UUID,
 status RequestedDrinkStatus
);