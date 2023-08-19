ALTER TABLE repositories ADD COLUMN ts tsvector
    GENERATED ALWAYS AS
     (
    setweight(to_tsvector('english', coalesce(headline, '')), 'A') ||
    setweight(to_tsvector('english', coalesce(summary, '')), 'B') ||
    setweight(to_tsvector('english', coalesce(readme_markdown, '')), 'C')
) STORED;
