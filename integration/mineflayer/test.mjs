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

function componentText(component) {
  if (typeof component === 'string') return component
  if (!component || typeof component !== 'object') return ''
  if (typeof component.value === 'string') return component.value
  if (component.type === 'compound' && component.value) {
    const text = componentText(component.value.text)
    const extra = Array.isArray(component.value.extra)
      ? component.value.extra.map(componentText).join('')
      : ''
    return text + extra
  }
  if (typeof component.text === 'string') return component.text
  if (Array.isArray(component.extra)) return component.extra.map(componentText).join('')
  return ''
}

function waitForTextDisplayText(bot, expected) {
  const textIndex = bot.registry.entitiesByName.text_display.metadataKeys.indexOf('text')
  if (textIndex < 0) {
    return Promise.reject(new Error('Mineflayer registry does not expose Text Display text metadata'))
  }

  return new Promise((resolve, reject) => {
    const matches = entity => entity.name === 'text_display'
      && componentText(entity.metadata[textIndex]) === expected
    const current = Object.values(bot.entities).find(matches)
    if (current) {
      resolve(current)
      return
    }

    const timeout = setTimeout(() => {
      bot.removeListener('entitySpawn', listener)
      bot.removeListener('entityUpdate', listener)
      const displays = Object.values(bot.entities)
        .filter(entity => entity.name === 'text_display')
      const uniqueTexts = [...new Map(displays
        .map(entity => ({
          id: entity.id,
          parsed: componentText(entity.metadata[textIndex]),
          raw: entity.metadata[textIndex]
        }))
        .filter(entry => entry.raw !== undefined)
        .map(entry => [JSON.stringify(entry.raw), entry]))
        .values()]
      reject(new Error(`Timed out waiting for shared label text: ${expected}; displays=${displays.length}; uniqueTexts=${JSON.stringify(uniqueTexts)}`))
    }, timeoutMs)
    const listener = entity => {
      if (!matches(entity)) return
      clearTimeout(timeout)
      bot.removeListener('entitySpawn', listener)
      bot.removeListener('entityUpdate', listener)
      resolve(entity)
    }
    bot.on('entitySpawn', listener)
    bot.on('entityUpdate', listener)
  })
}

function waitForTextDisplayCount(bot, expectedMinimum) {
  const count = () => Object.values(bot.entities)
    .filter(entity => entity.name === 'text_display')
    .length

  return new Promise((resolve, reject) => {
    const current = count()
    if (current >= expectedMinimum) {
      resolve(current)
      return
    }

    const timeout = setTimeout(() => {
      bot.removeListener('entitySpawn', listener)
      reject(new Error(`Timed out waiting for ${expectedMinimum} Text Display entities; received ${count()}`))
    }, timeoutMs)
    const listener = entity => {
      if (entity.name !== 'text_display') return
      const currentCount = count()
      if (currentCount < expectedMinimum) return
      clearTimeout(timeout)
      bot.removeListener('entitySpawn', listener)
      resolve(currentCount)
    }
    bot.on('entitySpawn', listener)
  })
}

function waitForCuiPayload(bot, expected) {
  return new Promise((resolve, reject) => {
    const timeout = setTimeout(() => {
      bot._client.removeListener('custom_payload', listener)
      reject(new Error(`Timed out waiting for WorldEditCUI payload: ${expected}`))
    }, timeoutMs)
    const listener = (packet) => {
      if (packet.channel !== 'worldedit:cui' || packet.data.toString('utf8') !== expected) return
      clearTimeout(timeout)
      bot._client.removeListener('custom_payload', listener)
      resolve(packet)
    }
    bot._client.on('custom_payload', listener)
  })
}

async function readCuiState(bot) {
  const stateMessage = waitForMessage(bot, 'WED_CUI_STATE:')
  bot.chat('/wedtest cui-state')
  const message = await stateMessage
  const state = message
    .substring(message.indexOf('WED_CUI_STATE:') + 'WED_CUI_STATE:'.length)
    .trim()
    .split(':')
  if (state.length !== 3 || !['true', 'false'].includes(state[0]) || !['true', 'false'].includes(state[1])) {
    throw new Error(`WorldEditDisplay reported invalid CUI state: ${message}`)
  }
  const entityCount = Number(state[2])
  if (!Number.isInteger(entityCount)) {
    throw new Error(`WorldEditDisplay reported invalid CUI entity count: ${message}`)
  }
  return {
    cuiEnabled: state[0] === 'true',
    renderingEnabled: state[1] === 'true',
    entityCount
  }
}

function sendCuiHandshake(bot) {
  bot._client.write('custom_payload', {
    channel: 'worldedit:cui',
    data: Buffer.from('v|4', 'utf8')
  })
}

function createAndSpawnBot(username) {
  const bot = mineflayer.createBot({
    host: '127.0.0.1',
    port,
    username,
    version: '1.21.11',
    auth: 'offline'
  })

  return new Promise((resolve, reject) => {
    const timeout = setTimeout(() => fail(new Error(`Timed out waiting for ${username} spawn`)), timeoutMs)
    const cleanup = () => {
      clearTimeout(timeout)
      bot.removeListener('spawn', spawned)
      bot.removeListener('error', fail)
      bot.removeListener('kicked', kicked)
    }
    const spawned = () => {
      cleanup()
      resolve(bot)
    }
    const fail = error => {
      cleanup()
      reject(error)
    }
    const kicked = reason => fail(new Error(`${username} was kicked: ${reason}`))
    bot.once('spawn', spawned)
    bot.once('error', fail)
    bot.once('kicked', kicked)
  })
}

const bots = []
const fatal = (error) => {
  console.error(error)
  process.exitCode = 1
  for (const bot of bots) bot.quit('e2e failed')
}

try {
  const sharer = await createAndSpawnBot('WEDSharer')
  bots.push(sharer)
  const viewer = await createAndSpawnBot('WEDViewer')
  bots.push(viewer)

  sharer.once('error', fatal)
  sharer.once('kicked', reason => fatal(new Error(`WEDSharer was kicked: ${reason}`)))
  viewer.once('error', fatal)
  viewer.once('kicked', reason => fatal(new Error(`WEDViewer was kicked: ${reason}`)))

  const debugEnabledMessage = waitForMessage(sharer, 'Debug mode enabled')
  sharer.chat('/wedisplay debug')
  await debugEnabledMessage

  const readyMessage = waitForMessage(sharer, 'WED_READY:')
  const retainedLineCountMessage = waitForMessage(sharer, 'Retained line shapes:')
  const retainedLinePassMessage = waitForMessage(sharer, 'Last line pass reused/spawned/removed:')
  sharer.chat('/wedtest')
  const ready = await readyMessage
  await Promise.all([retainedLineCountMessage, retainedLinePassMessage])

  const state = ready
    .substring(ready.indexOf('WED_READY:') + 'WED_READY:'.length)
    .trim()
    .split(':')
    .map(Number)
  if (state.length !== 6 || state.some(value => !Number.isInteger(value))) {
    throw new Error(`WorldEditDisplay reported an invalid entity count: ${ready}`)
  }
  const [entityCount, retainedLineCount, retainedLineEntityCount, reusedLines, spawnedLines, removedLines] = state
  if (entityCount <= 0 || retainedLineCount <= 0 || retainedLineEntityCount <= 0) {
    throw new Error(`WorldEditDisplay reported invalid retained renderer state: ${ready}`)
  }
  if (reusedLines <= 0 || spawnedLines !== 0 || removedLines !== 0) {
    throw new Error(`WorldEditDisplay did not reuse the retained VirtualEntities shapes: ${ready}`)
  }

  const entity = await waitForTextDisplay(sharer)
  const translationIndex = sharer.registry.entitiesByName.text_display.metadataKeys.indexOf('translation')
  if (translationIndex < 0) {
    throw new Error('Mineflayer registry does not expose Text Display translation metadata')
  }
  await waitForMetadata(
    sharer,
    entity,
    current => Number.isFinite(current.metadata[translationIndex]?.x),
    'WorldEditDisplay Text Display translation metadata'
  )

  const visibleTextDisplays = Object.values(sharer.entities)
    .filter(current => current.name === 'text_display')
    .length
  if (visibleTextDisplays <= 0) {
    throw new Error('WorldEditDisplay did not leave any visible Text Display entities')
  }

  const inviteReceived = waitForMessage(viewer, 'wants to share their selection with you')
  sharer.chat('/wedisplay share invite WEDViewer')
  await inviteReceived

  const sharedLabelPromise = waitForTextDisplayText(viewer, 'WEDSharer')
  const acceptedMessage = waitForMessage(viewer, 'You are now viewing WEDSharer')
  viewer.chat('/wedisplay share accept WEDSharer')
  await acceptedMessage
  const sharedLabel = await sharedLabelPromise
  const viewerTextDisplays = await waitForTextDisplayCount(viewer, visibleTextDisplays + 1)

  const regularClientCuiState = await readCuiState(sharer)
  if (regularClientCuiState.cuiEnabled) {
    throw new Error('WorldEditDisplay mistook its silent server handshake for a client-side CUI')
  }

  const cuiClient = await createAndSpawnBot('WEDCUI')
  bots.push(cuiClient)
  cuiClient.once('error', fatal)
  cuiClient.once('kicked', reason => fatal(new Error(`WEDCUI was kicked: ${reason}`)))

  const cuiRendererReadyMessage = waitForMessage(cuiClient, 'WED_READY:')
  cuiClient.chat('/wedtest')
  const cuiRendererReady = await cuiRendererReadyMessage
  const cuiRendererEntityCount = Number(
    cuiRendererReady
      .substring(cuiRendererReady.indexOf('WED_READY:') + 'WED_READY:'.length)
      .trim()
      .split(':')[0]
  )
  if (!Number.isInteger(cuiRendererEntityCount) || cuiRendererEntityCount <= 0) {
    throw new Error(`WorldEditDisplay did not render for a client before its CUI handshake: ${cuiRendererReady}`)
  }

  sendCuiHandshake(cuiClient)
  const nativeCuiState = await readCuiState(cuiClient)
  if (!nativeCuiState.cuiEnabled || !nativeCuiState.renderingEnabled || nativeCuiState.entityCount !== 0) {
    throw new Error(`WorldEditDisplay did not hand rendering over to the native CUI: ${JSON.stringify(nativeCuiState)}`)
  }

  const forwardedCuiPayload = waitForCuiPayload(cuiClient, 'u|0')
  cuiClient.chat('/wedtest cui-forward')
  await forwardedCuiPayload

  const renderingDisabledMessage = waitForMessage(cuiClient, 'selection rendering disabled')
  cuiClient.chat('/wedisplay toggle')
  await renderingDisabledMessage
  const renderingEnabledMessage = waitForMessage(cuiClient, 'selection rendering enabled')
  cuiClient.chat('/wedisplay toggle')
  await renderingEnabledMessage
  const refreshedNativeCuiState = await readCuiState(cuiClient)
  if (!refreshedNativeCuiState.cuiEnabled
      || !refreshedNativeCuiState.renderingEnabled
      || refreshedNativeCuiState.entityCount !== 0) {
    throw new Error(`WorldEditDisplay recreated its renderer after a native CUI refresh: ${JSON.stringify(refreshedNativeCuiState)}`)
  }

  console.log(JSON.stringify({
    managerInitialized: true,
    rendererEntities: entityCount,
    retainedLineCount,
    retainedLineEntityCount,
    retainedLinePass: { reusedLines, spawnedLines, removedLines },
    debugMessagesObserved: true,
    visibleTextDisplays,
    sharedViewerTextDisplays: viewerTextDisplays,
    sharedLabelEntityId: sharedLabel.id,
    sharedLabelText: 'WEDSharer',
    regularClientCuiDetected: regularClientCuiState.cuiEnabled,
    nativeCuiDetected: nativeCuiState.cuiEnabled,
    nativeCuiRendererCleared: nativeCuiState.entityCount === 0,
    nativeCuiPayloadForwarded: true,
    nativeCuiRefreshSuppressed: refreshedNativeCuiState.entityCount === 0,
    translation: entity.metadata[translationIndex]
  }))
  sharer.quit('e2e complete')
  viewer.quit('e2e complete')
  cuiClient.quit('e2e complete')
} catch (error) {
  fatal(error)
}
