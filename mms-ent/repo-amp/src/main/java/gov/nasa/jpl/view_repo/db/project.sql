-- Create tables
create table nodeTypes
(
  id bigserial primary key,
  name text not null
);

create table edgeTypes
(
  id bigserial primary key,
  name text not null
);

create table nodes
(
  id bigserial primary key,
  elasticId text not null unique,
  nodeType integer references nodeTypes(id) not null,
  sysmlId text not null unique
);
create index nodeIndex on nodes(id);
create index sysmlIndex on nodes(sysmlId);

create table edges
(
  id bigserial primary key,
  parent integer references nodes(id) not null,
  child integer references nodes(id) not null,
  edgeType integer references edgeTypes(id) not null,
  constraint unique_edges unique(parent, child, edgeType)
);
create index edgeIndex on edges(id);

create table edgeProperties
(
  edgeId integer references edges(id) ON DELETE CASCADE not null,
  key text not null,
  value text not null,
  constraint unique_edgeproperties unique(edgeId, key)
);


create table commit_type
(
  id bigserial primary key,
  name text not null
);

create table commits
(
  id  text primary key,
  commit_reference_id text not null unique,
  timestamp timestamp default current_timestamp,
  commit_type bigserial,
  creator text,
  FOREIGN KEY (commit_type) REFERENCES Commit_Type(id) ON DELETE CASCADE
);

create index commitIndex on commits(commit_reference_id);

create table commit_parent
(
  commit_id text NOT NULL,
  child_ws text,
  parent_id text,
  parent_ws text,
  FOREIGN KEY (commit_id) REFERENCES commits(id) ON DELETE CASCADE,
  FOREIGN KEY (parent_id) REFERENCES commits(id) ON DELETE CASCADE,
  constraint unique_parents unique(commit_id, parent_id)
);

create index commitType on commit_type(id);

create table workspaces
(
  id bigserial primary key,
  parent text not null,
  timestamp timestamp default current_timestamp
);

create table configurations
(
  id bigserial primary key,
  elasticId text not null unique,
  name text not null unique,
  timestamp timestamp default current_timestamp
);

create index childIndex on edges (child);
create index parentIndex on edges (parent);

-- given two elasticId, insert an edge between the two
create or replace function insert_edge(text, text, text, integer)
  returns integer as $$
  begin
    execute '
      insert into ' || (format('edges%s', $3)) || ' (parent, child, edgeType) values((select id from ' || format('nodes%s',$3) || ' where sysmlId = ''' || $1 || '''), (select id from ' || format('nodes%s', $3) || ' where sysmlId = ''' || $2 || '''), ' || $4 || ');';
      return 1;
    exception
      when unique_violation then
        return -1;
      when not_null_violation then
        return -1;
  end;
$$ language plpgsql;

-- insert edge property
create or replace function insert_edge_property(text, text, text, integer, text, text)
  returns integer as $$
  begin
  execute '
      insert into ' || (format('edgeProperties%s', $3)) || ' (edgeId, key, value) values((select id from ' || format('edges%s', $3) || ' where parent = (select id from ' || format('nodes%s', $3) || ' where sysmlId = ''' || $1 || ''') and child = (select id from ' || format('nodes%s', $3) || ' where sysmlId = ''' || $2 || ''') and edgeType = ' || $4 || '), ''' || $5 || ''', ''' || $6 || ''');';
      return 1;
    exception
      when unique_violation then
        return -1;
      when not_null_violation then
        return -1;
  end;
$$ language plpgsql;

-- get all edge properties
-- arguments
--  integer edgeid
create or replace function get_edge_properties(edge integer)
  returns table(key text, value text) as $$
  begin
    return query
    execute '
      select key, value from edgeProperties where edgeid = ' || edge;
  end;
$$ language plpgsql;

-- recursively get all children including oneself
-- arguments
--  integer sysmlid
--  integer edgetype
--  text    workspace
--  integer depth
create or replace function get_children(integer, integer, text, integer)
  returns table(id bigint) as $$
  begin
    return query
    execute '
    with recursive children(depth, nid, path, cycle) as (
      select 0 as depth, node.id, ARRAY[node.id], false from ' || format('nodes%s', $3) || '
        node where node.id = ' || $1 || '
      union
      select (c.depth + 1) as depth, edge.child as nid, path || cast(edge.child as bigint) as path, edge.child = ANY(path) as cycle
        from ' || format('edges%s', $3) || ' edge, children c where edge.parent = nid and
        edge.edgeType = ' || $2 || ' and not cycle and depth < ' || $4 || '
      )
      select distinct nid from children;';
  end;
$$ language plpgsql;

-- recursively get all childviews
-- arguments
--  integer sysmlid
--  text    workspace
create or replace function get_childviews(integer, text)
  returns table(sysmlid text, aggregation text) as $$
  begin
    return query
    execute '
    with childviews(sysmlid, aggregation) as (
        (
        select typeid.value as sysmlid, aggregation.value as aggregation
          from ' || format('edges%s', $2) || ' as edges
          join ' || format('edgeProperties%s', $2) || ' as ordering on edges.id = ordering.edgeid and ordering.key = ''order''
          join ' || format('edgeProperties%s', $2) || ' as aggregation on edges.id = aggregation.edgeid and aggregation.key = ''aggregation''
          join ' || format('edgeProperties%s', $2) || ' as typeid on edges.id = typeid.edgeid and typeid.key = ''typeId''
          join ' || format('nodes%s', $2) || ' as child on typeid.value = child.sysmlid and (child.nodetype = 4 or child.nodetype = 12)
          where edges.parent = ' || $1 || '
          order by ordering.value ASC
        )
      )
      select sysmlid, aggregation from childviews;';
  end;
$$ language plpgsql;

create or replace function get_parents(integer, integer, text)
  returns table(id bigint, height integer, root boolean) as $$
  begin
    return query
    execute '
    with recursive parents(height, nid, path, cycle) as (
    select 0, node.id, ARRAY[node.id], false from ' || format('nodes%s', $3) || ' node where node.id = ' || $1 || '
    union
      select (c.height + 1), edge.parent, path || cast(edge.parent as bigint),
        edge.parent = ANY(path) from ' || format('edges%s', $3) || '
        edge, parents c where edge.child = nid and edge.edgeType = ' || $2 || '
        and not cycle
      )
      select nid,height,(not exists (select true from edges where child = nid and edgetype = ' || $2 || '))
        from parents order by height desc;';
  end;
$$ language plpgsql;

create or replace function get_immediate_parents(integer, integer, text)
  returns table(sysmlid text, elasticid text) as $$
  begin
    return query
    execute '
    select sysmlid, elasticid from nodes' || $3 || ' where id in
      (select id from get_parents(' || $1 || ',' || $2 || ',''' || format('%s',$3) ||
      ''') where height = 1);';
  end;
$$ language plpgsql;

create or replace function get_root_parents(integer, integer, text)
  returns table(sysmlid text) as $$
  begin
    return query
    execute '
    select sysmlid from nodes' || $3 || ' where id in
      (select id from get_parents(' || $1 || ',' || $2 || ',''' || format('%s',$3) ||
      ''') where root = true);';
  end;
$$ language plpgsql;

-- get root parents of immediate parents
create or replace function get_immediate_parent_roots(integer, integer, text)
  returns table(ip text, rp text) as $$
  declare
    s text;
    l text;
  begin
    FOR s in select sysmlid from get_immediate_parents($1,$2,$3) LOOP
      return query select s,sysmlid from get_root_parents(cast((select id from nodes where sysmlid=s) as int), $2, $3);
    end Loop;
    RETURN;
 end;
$$ language plpgsql;

-- get all paths to a node
create type return_type as (pstart integer, pend integer, path integer[]);
create or replace function get_paths_to_node(integer, integer, text)
  returns setof return_type as $$
  begin
    return query
    execute '
    with recursive node_graph as (
      select parent as path_start, child as path_end,
             array[parent, child] as path
      from ' || format('edges%s', $3) || ' where edgeType = ' || $2 || '
      union all
      select ng.path_start, nr.child as path_end,
           ng.path || nr.child as path
      from node_graph ng
      join edges nr ON ng.path_end = nr.parent where nr.edgeType = ' || $2 || '
    )
    select * from node_graph where path_end = ' || $1 || ' order by path_start, array_length(path,1)';
  end;
$$ language plpgsql;
-- Gets documents by the group level
CREATE OR REPLACE FUNCTION get_group_docs(integer, integer, text, integer, integer, integer)
    RETURNS table(id bigint) AS $$
    BEGIN
        RETURN query
        EXECUTE '
        WITH RECURSIVE children(depth, nid, path, cycle, deleted, ntype) as (
            select 0 as depth, node.id, ARRAY[node.id], false, node.deleted, node.nodetype from ' || format('nodes%s', $3) || '
            node where node.id = ' || $1 || '
            UNION
            select (c.depth + 1) as depth, edge.child as nid, path || cast(edge.child as bigint) as path, edge.child = ANY(path) as cycle, node.deleted as deleted, node.nodetype as ntype
            from ' || format('edges%s', $3) || ' edge, children c, ' || format('nodes%s', $3) || ' node where edge.parent = nid and node.id = edge.child and node.deleted = false and
            edge.edgeType = ' || $2 || ' and not cycle and depth < ' || $4 || 'and (node.nodetype <> '|| $5 ||' or nid = ' || $1 || ')
        )
        select distinct nid from children where ntype = ' || $6 || ';' ;
    END;
$$ language plpgsql;

-- get all root parents of a node
create aggregate array_agg_mult(anyarray) (
    SFUNC = array_cat,
    STYPE = anyarray,
    INITCOND = '{}'
);

create or replace function array_sort_unique (anyarray)
  returns anyarray
  as $body$
    select array(
      select distinct $1[s.i]
      from generate_series(array_lower($1,1), array_upper($1,1)) AS s(i)
      order by 1
    );
  $body$
language sql;

insert into nodeTypes(name) values ('element');
insert into nodeTypes(name) values ('site');
insert into nodeTypes(name) values ('project');
insert into nodeTypes(name) values ('document');
insert into nodeTypes(name) values ('comment');
insert into nodeTypes(name) values ('constraint');
insert into nodeTypes(name) values ('instancespecification');
insert into nodeTypes(name) values ('operation');
insert into nodeTypes(name) values ('package');
insert into nodeTypes(name) values ('property');
insert into nodeTypes(name) values ('parameter');
insert into nodeTypes(name) values ('view');
insert into nodeTypes(name) values ('viewpoint');
insert into nodeTypes(name) values ('siteandpackage');
insert into nodeTypes(name) values ('holdingbin');

insert into edgeTypes(name) values ('containment');
insert into edgeTypes(name) values ('view');
insert into edgeTypes(name) values ('transclusion');
insert into edgeTypes(name) values ('childview');

insert into nodes(elasticId, nodeType, sysmlId) values ('holding_bin', (select nodeTypes.id from nodeTypes where name = 'holdingbin'), 'holding_bin');

insert into commit_type(name) values ('commit');
insert into commit_type(name) values ('branch');
insert into commit_type(name) values ('merge');


GRANT ALL PRIVILEGES ON DATABASE mms to mmsuser;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO mmsuser;
GRANT USAGE, SELECT ON SEQUENCE nodes_id_seq TO mmsuser;
GRANT USAGE, SELECT ON SEQUENCE configurations_id_seq TO mmsuser;
GRANT USAGE, SELECT ON SEQUENCE workspaces_id_seq TO mmsuser;

-- test data etc. comment out when not in dev mode

/*
DO
$do$
BEGIN
FOR i IN 1..250000 LOOP
  execute format('insert into nodes(elasticId, nodeType, sysmlId) values(%s, 1, (select nodeTypes.id from nodeTypes where name = ''regular''), %s)', i, i);
END LOOP;
END
$do$;


DO
$do$
BEGIN
FOR i IN 1..250000 LOOP
  execute format('insert into edges values(%s, %s, 1)', floor(random() * 250000) + 1, floor(random()*250000) + 1);
END LOOP;
END
$do$;

insert into nodes(elasticId, versionedRefId, nodeType, sysmlId) values('1', 1, (select nodeTypes.id from nodeTypes where name = 'regular'), '1');
insert into nodes(elasticId, versionedRefId, nodeType, sysmlId) values(2, 2, (select nodeTypes.id from nodeTypes where name = 'regular'), '2');
insert into nodes(elasticId, versionedRefId, nodeType, sysmlId) values(3, 3, (select nodeTypes.id from nodeTypes where name = 'regular'), '3');
insert into nodes(elasticId, versionedRefId, nodeType, sysmlId) values(4, 4, (select nodeTypes.id from nodeTypes where name = 'regular'), '4');
insert into nodes(elasticId, versionedRefId, nodeType, sysmlId) values(5, 5, (select nodeTypes.id from nodeTypes where name = 'regular'), '5');
insert into nodes(elasticId, versionedRefId, nodeType, sysmlId) values(6, 6, (select nodeTypes.id from nodeTypes where name = 'regular'), '6');
insert into nodes(elasticId, versionedRefId, nodeType, sysmlId) values(7, 7, (select nodeTypes.id from nodeTypes where name = 'regular'), '7');
insert into nodes(elasticId, versionedRefId, nodeType, sysmlId) values(8, 8, (select nodeTypes.id from nodeTypes where name = 'regular'), '8');

insert into edges values(1, 2, (select edgeTypes.id from edgeTypes where name = 'regular'));
insert into edges values(1, 3, (select edgeTypes.id from edgeTypes where name = 'regular'));
insert into edges values(2, 5, (select edgeTypes.id from edgeTypes where name = 'regular'));
insert into edges values(2, 6, (select edgeTypes.id from edgeTypes where name = 'regular'));
insert into edges values(3, 4, (select edgeTypes.id from edgeTypes where name = 'regular'));
insert into edges values(7, 2, (select edgeTypes.id from edgeTypes where name = 'regular'));
insert into edges values(3, 8, (select edgeTypes.id from edgeTypes where name = 'regular'));
insert into edges values(8, 4, (select edgeTypes.id from edgeTypes where name = 'regular'));

insert into edges values(1, 2, (select edgeTypes.id from edgeTypes where name = 'document'));
insert into edges values(2, 6, (select edgeTypes.id from edgeTypes where name = 'document'));
insert into edges values(7, 2, (select edgeTypes.id from edgeTypes where name = 'document'));
insert into edges values(6, 4, (select edgeTypes.id from edgeTypes where name = 'document'));

*/
