drop database projectdb;
create database projectdb;
\c projectdb


CREATE TABLE trains_released(
    T_number VARCHAR(100) NOT NULL,
    Date DATE NOT NULL,
    N_AC INTEGER NOT NULL,
    N_SL INTEGER NOT NULL,
    PRIMARY KEY(T_number, Date)
);


CREATE TABLE ac_coach(
    b_number INTEGER NOT NULL PRIMARY KEY,
    b_type VARCHAR(3)
);

INSERT INTO ac_coach(b_number, b_type) VALUES (1,'LB');
INSERT INTO ac_coach(b_number, b_type) VALUES (2,'LB');
INSERT INTO ac_coach(b_number, b_type) VALUES (3,'UB');
INSERT INTO ac_coach(b_number, b_type) VALUES (4,'UB');
INSERT INTO ac_coach(b_number, b_type) VALUES (5,'SL');
INSERT INTO ac_coach(b_number, b_type) VALUES (6,'SU');
INSERT INTO ac_coach(b_number, b_type) VALUES (7,'LB');
INSERT INTO ac_coach(b_number, b_type) VALUES (8,'LB');
INSERT INTO ac_coach(b_number, b_type) VALUES (9,'UB');
INSERT INTO ac_coach(b_number, b_type) VALUES (10,'UB');
INSERT INTO ac_coach(b_number, b_type) VALUES (11,'SL');
INSERT INTO ac_coach(b_number, b_type) VALUES (12,'SU');
INSERT INTO ac_coach(b_number, b_type) VALUES (13,'LB');
INSERT INTO ac_coach(b_number, b_type) VALUES (14,'LB');
INSERT INTO ac_coach(b_number, b_type) VALUES (15,'UB');
INSERT INTO ac_coach(b_number, b_type) VALUES (16,'UB');
INSERT INTO ac_coach(b_number, b_type) VALUES (17,'SL');
INSERT INTO ac_coach(b_number, b_type) VALUES (18,'SU');



CREATE TABLE sl_coach(
    b_number INTEGER NOT NULL PRIMARY KEY,
    b_type VARCHAR(3)
);

INSERT INTO sl_coach(b_number, b_type) VALUES (1,'LB');
INSERT INTO sl_coach(b_number, b_type) VALUES (2,'MB');
INSERT INTO sl_coach(b_number, b_type) VALUES (3,'UB');
INSERT INTO sl_coach(b_number, b_type) VALUES (4,'LB');
INSERT INTO sl_coach(b_number, b_type) VALUES (5,'MB');
INSERT INTO sl_coach(b_number, b_type) VALUES (6,'UB');
INSERT INTO sl_coach(b_number, b_type) VALUES (7,'SL');
INSERT INTO sl_coach(b_number, b_type) VALUES (8,'SU');
INSERT INTO sl_coach(b_number, b_type) VALUES (9,'LB');
INSERT INTO sl_coach(b_number, b_type) VALUES (10,'MB');
INSERT INTO sl_coach(b_number, b_type) VALUES (11,'UB');
INSERT INTO sl_coach(b_number, b_type) VALUES (12,'LB');
INSERT INTO sl_coach(b_number, b_type) VALUES (13,'MB');
INSERT INTO sl_coach(b_number, b_type) VALUES (14,'UB');
INSERT INTO sl_coach(b_number, b_type) VALUES (15,'SL');
INSERT INTO sl_coach(b_number, b_type) VALUES (16,'SU');
INSERT INTO sl_coach(b_number, b_type) VALUES (17,'LB');
INSERT INTO sl_coach(b_number, b_type) VALUES (18,'MB');
INSERT INTO sl_coach(b_number, b_type) VALUES (19,'UB');
INSERT INTO sl_coach(b_number, b_type) VALUES (20,'LB');
INSERT INTO sl_coach(b_number, b_type) VALUES (21,'MB');
INSERT INTO sl_coach(b_number, b_type) VALUES (22,'UB');
INSERT INTO sl_coach(b_number, b_type) VALUES (23,'SL');
INSERT INTO sl_coach(b_number, b_type) VALUES (24,'SU');



CREATE TABLE avail_seats (
    t_number VARCHAR(100) NOT NULL,
    date DATE NOT NULL,
    c_number INTEGER NOT NULL,
    c_type VARCHAR(3) NOT NULL,
    avail_seats INTEGER NOT NULL,
    PRIMARY KEY(t_number, date, c_number, c_type),
    FOREIGN KEY(t_number,date) REFERENCES trains_released(t_number,date)
);

CREATE TABLE ticket_seats(
    PNR VARCHAR(100) NOT NULL,
    t_number VARCHAR(100) NOT NULL,
    date DATE NOT NULL,
    c_type VARCHAR(3) NOT NULL,
    PRIMARY KEY(PNR),
    FOREIGN KEY(t_number,date) REFERENCES trains_released(t_number,date)
);


CREATE TABLE ticket_passengers(
    PNR VARCHAR(100) NOT NULL,
    P_name VARCHAR(30)  NOT NULL,
    c_number INTEGER NOT NULL,
    b_number INTEGER NOT NULL,
    PRIMARY KEY(PNR, b_number),
    FOREIGN KEY(PNR) REFERENCES ticket_seats(PNR)
);

CREATE OR REPLACE FUNCTION add_coaches()
RETURNS TRIGGER as $$
DECLARE
    N_AC INTEGER DEFAULT 0;
    N_SL INTEGER DEFAULT 0;
BEGIN

    N_AC = NEW.n_ac;
    N_SL = NEW.n_sl;

    for cnt in 1..N_AC
    LOOP
        INSERT INTO avail_seats(t_number, date, c_number, c_type, avail_seats) VALUES (NEW.t_number, NEW.date, cnt, 'AC', 18);
    END LOOP;

    for cnt in 1..N_SL
    LOOP
        INSERT INTO avail_seats(t_number, date, c_number, c_type, avail_seats) VALUES (NEW.t_number, NEW.date, N_AC+cnt, 'SL', 24);
    END LOOP;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER train_insert
    AFTER INSERT
    ON trains_released
    FOR EACH ROW
EXECUTE PROCEDURE add_coaches();

CREATE OR REPLACE FUNCTION book_ticket(train_number text, date_of_journey DATE, coach_type text, n_pass INTEGER, names text[])
RETURNS TEXT AS $$
    DECLARE
        seats_required INTEGER DEFAULT n_pass;
        seats_available INTEGER DEFAULT 0;
        current_passenger INTEGER DEFAULT 0;
        cap_per_coach INTEGER DEFAULT 24;
        current_bnumber  INTEGER DEFAULT 0;
        format_date VARCHAR(20) DEFAULT date_of_journey;
        PNRNUM VARCHAR(100) DEFAULT 'ABC';
        coach_row RECORD;
    BEGIN
        SELECT INTO seats_available COALESCE(SUM(avail_seats),0) as n_seats FROM avail_seats where t_number = train_number and date = date_of_journey and c_type = coach_type;
        IF (n_pass > seats_available)
        THEN
            RETURN '-1';
        END IF;

        IF (coach_type = 'AC')
        THEN cap_per_coach = 18;
        END IF;
        FOR coach_row IN SELECT * FROM avail_seats WHERE t_number = train_number and date = date_of_journey and c_type  = coach_type ORDER BY avail_seats
        LOOP
            current_bnumber = cap_per_coach - coach_row.avail_seats + 1;
            
            IF (PNRNUM = 'ABC')
            THEN PNRNUM = train_number || '-' || translate(format_date, '- ', '') || '-' || coach_row.c_number || '-' || current_bnumber  ;
            INSERT INTO ticket_seats(PNR,t_number, date, c_type) VALUES (PNRNUM, train_number, date_of_journey, coach_type);
            END IF;
            
            FOR cnt IN current_passenger..n_pass-1
            LOOP
                INSERT INTO ticket_passengers(PNR,P_name,c_number,b_number) VALUES (PNRNUM, names[cnt+1], coach_row.c_number,current_bnumber);
                current_bnumber = current_bnumber +1;
                seats_required  = seats_required - 1;
                current_passenger = current_passenger + 1;
                
                IF (current_bnumber > cap_per_coach)
                THEN EXIT;
                END IF;

            END LOOP;
        
        IF (current_bnumber > cap_per_coach)
        THEN DELETE FROM avail_seats WHERE t_number = train_number and date = date_of_journey and c_type = coach_type and c_number = coach_row.c_number;
        ELSE UPDATE avail_seats SET avail_seats  = (cap_per_coach-current_bnumber+1) WHERE t_number = train_number and date = date_of_journey and c_type = coach_type and c_number = coach_row.c_number;
        END IF;

        IF (seats_required  <= 0)
        THEN EXIT;
        END IF;

        END LOOP;
        RETURN PNRNUM;
    END;
$$ LANGUAGE plpgsql;
