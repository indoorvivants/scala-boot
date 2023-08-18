-- create
CREATE TABLE repositories (
  repoId serial primary key,
  name text UNIQUE not null,
  headline text,
  summary text,
  readme_markdown text not null,
  metadata json,
  last_commit varchar(40)
);

