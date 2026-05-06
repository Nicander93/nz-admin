import * as userApi from './user'
import * as deptApi from './dept'
import * as roleApi from './role'
import * as postApi from './post'
import * as configApi from './config'
import * as noticeApi from './notice'

export { userApi, deptApi, roleApi, postApi, configApi, noticeApi }

export type { SysUser, UserQuery } from './user'
export type { SysDept } from './dept'
export type { SysRole } from './role'
export type { SysPost } from './post'
export type { SysConfig } from './config'
export type { SysNotice, NoticeQuery } from './notice'
