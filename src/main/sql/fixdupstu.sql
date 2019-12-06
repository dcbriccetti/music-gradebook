-- Fix a student with duplicate records


select * from musician where last_name = 'Rose';

-- Show musician with group history
select *
from musician m
         left join musician_group on musician_id = m.id
where m.id in (
    select id
    from musician
    where last_name = 'Rose'
)
order by m.id;

-- Get the number of assessments
select musician_id, count(musician_id)
from assessment
where musician_id in (
    select id
    from musician
    where last_name = 'Rose'
)
group by musician_id;

-- Real musician ID 104256, wrong one: 104491
update assessment set musician_id=104256 where musician_id=104491;
update musician_group set musician_id=104256 where musician_id=104491;
delete from musician_group where musician_id=104491;
delete from musician where id=104491;
