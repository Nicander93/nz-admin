import { cp, mkdir, mkdtemp } from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

export const sourceRoot = fileURLToPath(new URL('../../../', import.meta.url))

async function copy(relative, root) {
  const source = path.join(sourceRoot, relative)
  const target = path.join(root, relative)
  await mkdir(path.dirname(target), { recursive: true })
  await cp(source, target, { recursive: true })
}

export async function createProjectFixture() {
  const root = await mkdtemp(path.join(os.tmpdir(), 'nz-cli-test-'))
  await copy('nz-server/mvnw', root)
  await copy('nz-server/mvnw.cmd', root)
  await copy('nz-server/pom.xml', root)
  await copy('nz-server/nz-module/pom.xml', root)
  await copy('nz-server/nz-app/pom.xml', root)
  await copy('nz-server/nz-app/src/main/resources/application.yml', root)
  await copy('nz-server/nz-app/src/main/resources/db', root)
  await copy('nz-server/nz-app/src/test/java/com/nz/admin/migration/FlywayMigrationResourcesTest.java', root)
  await copy('nz-server/nz-module/nz-demo/src/main/resources/META-INF/nz/module.yaml', root)
  await copy('nz-web/package.json', root)
  return root
}
