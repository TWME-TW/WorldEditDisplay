import mineflayer from 'mineflayer'

const port = Number(process.env.WED_E2E_PORT ?? 25579)
const timeoutMs = 45_000

function waitForMessage(bot, expected) {
  return new Promise((resolve, reject) => {
    const timeout = setTimeout(() => {
      bot.removeListener('messagestr', listener)
      reject(new Error(`Timed out waiting for message: ${expected}`))
    }, timeoutMs)
    const listener = (message) => {
      if (!message.includes(expected)) return
      clearTimeout(timeout)
      bot.removeListener('messagestr', listener)
      resolve(message)
    }
    bot.on('messagestr', listener)
  })
}

function waitForTextDisplay(bot) {
  return new Promise((resolve, reject) => {
    const current = Object.values(bot.entities).find(entity => entity.name === 'text_display')
    if (current) {
      resolve(current)
      return
    }
    const timeout = setTimeout(() => {
      bot.removeListener('entitySpawn', listener)
      reject(new Error('Timed out waiting for a WorldEditDisplay text_display entity'))
    }, timeoutMs)
    const listener = (entity) => {
      if (entity.name !== 'text_display') return
      clearTimeout(timeout)
      bot.removeListener('entitySpawn', listener)
      resolve(entity)
    }
    bot.on('entitySpawn', listener)
  })
}

function waitForMetadata(bot, entity, predicate, description) {
  return new Promise((resolve, reject) => {
    if (predicate(entity)) {
      resolve(entity)
      return
    }
    const timeout = setTimeout(() => {
      bot.removeListener('entityUpdate', listener)
      reject(new Error(`Timed out waiting for ${description}`))
    }, timeoutMs)
    const listener = (updated) => {
      if (updated.id !== entity.id || !predicate(updated)) return
      clearTimeout(timeout)
      bot.removeListener('entityUpdate', listener)
      resolve(updated)
    }
    bot.on('entityUpdate', listener)
  })
}

const bot = mineflayer.createBot({
  host: '127.0.0.1',
  port,
  username: 'WEDClient',
  version: '1.21.11',
  auth: 'offline'
})

const fatal = (error) => {
  console.error(error)
  process.exitCode = 1
  bot.quit('e2e failed')
}

try {
  await new Promise((resolve, reject) => {
    const timeout = setTimeout(() => fail(new Error('Timed out waiting for Mineflayer spawn')), timeoutMs)
    const cleanup = () => {
      clearTimeout(timeout)
      bot.removeListener('spawn', spawned)
      bot.removeListener('error', fail)
      bot.removeListener('kicked', kicked)
    }
    const spawned = () => {
      cleanup()
      resolve()
    }
    const fail = (error) => {
      cleanup()
      reject(error)
    }
    const kicked = (reason) => fail(new Error(`Mineflayer was kicked: ${reason}`))
    bot.once('spawn', spawned)
    bot.once('error', fail)
    bot.once('kicked', kicked)
  })

  bot.once('error', fatal)
  bot.once('kicked', reason => fatal(new Error(`Mineflayer was kicked: ${reason}`)))

  const readyMessage = waitForMessage(bot, 'WED_READY:')
  bot.chat('/wedtest')
  const ready = await readyMessage
  const entityCount = Number(ready.substring(ready.indexOf('WED_READY:') + 'WED_READY:'.length).trim())
  if (!Number.isInteger(entityCount) || entityCount <= 0) {
    throw new Error(`WorldEditDisplay reported an invalid entity count: ${ready}`)
  }

  const entity = await waitForTextDisplay(bot)
  const translationIndex = bot.registry.entitiesByName.text_display.metadataKeys.indexOf('translation')
  if (translationIndex < 0) {
    throw new Error('Mineflayer registry does not expose Text Display translation metadata')
  }
  await waitForMetadata(
    bot,
    entity,
    current => Number.isFinite(current.metadata[translationIndex]?.x),
    'WorldEditDisplay Text Display translation metadata'
  )

  const visibleTextDisplays = Object.values(bot.entities)
    .filter(current => current.name === 'text_display')
    .length
  if (visibleTextDisplays <= 0) {
    throw new Error('WorldEditDisplay did not leave any visible Text Display entities')
  }

  console.log(JSON.stringify({
    managerInitialized: true,
    rendererEntities: entityCount,
    visibleTextDisplays,
    translation: entity.metadata[translationIndex]
  }))
  bot.quit('e2e complete')
} catch (error) {
  fatal(error)
}
