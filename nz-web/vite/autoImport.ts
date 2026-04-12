import AutoImport from 'unplugin-auto-import/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default AutoImport({
    resolvers: [ElementPlusResolver()],
    imports: [
        { '@/utils/CRUD': ['useTable', 'useCrud', 'useForm'] },
    ],
    dts: 'types/auto-imports.d.ts',
})
