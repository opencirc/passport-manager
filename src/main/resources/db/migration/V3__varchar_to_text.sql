alter table datasheet_property rename to datasheet_properties;

alter table datasheets alter column code type text;
alter table datasheets alter column name type text;
alter table datasheets alter column description type text;
alter table datasheets alter column platform_id type text;

alter table datasheet_properties alter column platform_id type text;
alter table datasheet_properties alter column group_tag type text;

alter table passport_logs alter column data type jsonb;

alter table passport_lifecycles alter column data type jsonb;
