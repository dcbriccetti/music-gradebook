insert into "GroupTerm" select "groupId", term+1, period from "GroupTerm" where term in (select max(term) from "GroupTerm");
