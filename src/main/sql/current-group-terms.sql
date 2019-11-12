select * from "GroupTerm" gt left join music_group mg on gt."groupId" = mg.id where term in (select max(term) from "GroupTerm") order by period;
