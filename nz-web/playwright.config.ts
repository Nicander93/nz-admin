import { browserTestConfig } from '@nz/test/config'
import dotenv from 'dotenv'

dotenv.config({ path: '.env.e2e.local' })
dotenv.config({ path: '.env.e2e' })

export default browserTestConfig()
