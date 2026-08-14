-- Job module: expose the independent job frontend route for existing installations.
UPDATE sys_menu
SET component = 'job/index', visible = 0
WHERE id = 2400 OR perm = 'system:job:list';