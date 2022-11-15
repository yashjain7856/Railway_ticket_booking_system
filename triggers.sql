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

CREATE OR REPLACE FUNCTION book_ticket(train_number INTEGER , date_of_journey DATE, coach_type text, n_pass INTEGER, names text[])
RETURNS INTEGER AS $$
    DECLARE
        seats_required INTEGER DEFAULT n_pass;
        seats_available INTEGER DEFAULT 0;
        current_passenger INTEGER DEFAULT 0;
        cap_per_coach INTEGER DEFAULT 24;
        current_bnumber  INTEGER DEFAULT 0;
        format_date VARCHAR(20) DEFAULT date_of_journey;
        PNRNUM VARCHAR(100) DEFAULT 'ABC';
        coach_row  RECORD;
    BEGIN
        
        SELECT INTO seats_available COALESCE(SUM(avail_seats),0) as n_seats FROM avail_seats where t_number = train_number and date = date_of_journey and c_type = coach_type;
        IF (n_pass > seats_available)
        THEN RETURN 0;
        END IF;

        IF (coach_type = 'AC')
        THEN cap_per_coach = 18;
        END IF;
        FOR coach_row IN SELECT * FROM avail_seats WHERE t_number = train_number and date = date_of_journey and c_type  = coach_type
        LOOP
            current_bnumber = cap_per_coach - coach_row.avail_seats + 1;
            
            IF (PNRNUM = 'ABC')
            THEN PNRNUM = train_number || translate(format_date, '- ', '') || coach_row.c_number || current_bnumber  ;
            END IF;
            raise notice '%',PNRNUM;

            INSERT INTO ticket_seats(PNR,t_number, date, c_type) VALUES (PNRNUM , train_number , date_of_journey, coach_type);
            FOR cnt IN current_passenger..n_pass-1
            LOOP
                raise notice '%',names[cnt+1];
                INSERT INTO ticket_passengers(PNR,P_name,c_number,b_number) VALUES (PNRNUM , names[cnt+1], coach_row.c_number,current_bnumber);
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
        RETURN 1;
    END;
$$ LANGUAGE plpgsql;

SELECT * FROM book_ticket (4567 , '2022-12-15','AC',100,'{ABCD,EFGH}');