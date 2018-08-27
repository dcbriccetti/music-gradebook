select * from "GroupTerm" gt left join music_group mg on gt."groupId" = mg.id where term = 2017 order by period;
