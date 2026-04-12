import { createCrudFactory } from '@nz-js-toolkit/crud'
import { ElMessage } from 'element-plus'

const { useTable, useCrud, useForm } = createCrudFactory({
  notify: (type, message) => {
    if (type === 'success') ElMessage.success(message)
    else if (type === 'error') ElMessage.error(message)
    else if (type === 'warning') ElMessage.warning(message)
  },
})

export { useTable, useCrud, useForm }
