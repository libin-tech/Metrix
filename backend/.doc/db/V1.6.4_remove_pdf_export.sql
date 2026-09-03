delete from system_role_api
where api_id in (
    select id from system_api where permission_code = 'analysis:record:pdf'
);

delete from system_menu_api
where api_id in (
    select id from system_api where permission_code = 'analysis:record:pdf'
);

delete from system_api
where permission_code = 'analysis:record:pdf';
