select * from musician_group left join musician m on musician_id = m.id where school_year = 2018 and group_id in (58562, 8) order by last_name, first_name;
