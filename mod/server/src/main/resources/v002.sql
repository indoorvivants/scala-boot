ALTER TABLE repositories ADD COLUMN ts tsvector
    GENERATED ALWAYS AS
     (
    setweight(to_tsvector('english', coalesce(name, '')), 'A') ||
    setweight(to_tsvector('english', coalesce(headline, '')), 'B') ||
    setweight(to_tsvector('english', coalesce(summary, '')), 'C') ||
    setweight(to_tsvector('english', coalesce(readme_markdown, '')), 'D')
) STORED;

CREATE INDEX repo_search ON repositories USING GIN(ts);
