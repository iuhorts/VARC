# Entrenador Virtual Roller — Documento de Arquitectura Funcional

> **Propósito:** Definir las Historias de Usuario (User Stories) detalladas que guiarán la construcción de una aplicación móvil (APK) de captación, análisis y puntuación automática de rutinas de Patinaje Artístico sobre ruedas, conforme al reglamento World Skate (Sistema Rollart).
>
> **Formato:** "Como [rol], quiero [acción], para [beneficio]" con Criterios de Aceptación (CA) asociados a cada historia.

---

## Historia de Usuario #1: Captación y Procesamiento de Vídeo con IA

| Campo | Valor |
|---|---|
| **ID** | HV‑01 |
| **Título** | Captación y Procesamiento de Vídeo con IA |
| **Rol** | Patinadora / Entrenador |
| **Rama Épica** | Núcleo de Visión por Computadora |
| **Dependencias** | Ninguna (sprint fundacional) |
| **Prioridad** | Crítica (MVP) |
| **Estimación** | 21 SP |

### Declaración

> **Como** patinadora o entrenador,
> **quiero** grabar en tiempo real o subir un vídeo desde la galería del dispositivo, y que el sistema lo procese mediante modelos de visión por computadora (estimación de pose 2D/3D, seguimiento articular y análisis cinemático) para identificar y etiquetar automáticamente cada elemento técnico ejecutado,
> **para** obtener un desglose instantáneo de los saltos, piruetas y secuencias de pasos sin necesidad de revisión manual.

### Criterios de Aceptación

#### CA‑01.01 — Captura en tiempo real
- La app expone una interfaz de cámara con superposición en vivo (viewfinder overlay) que muestre un *esqueleto* de pose estimada en tiempo real.
- El usuario puede iniciar/detener la grabación con un solo toque.
- La resolución mínima de captura es 720p a 30 FPS; se recomienda 1080p a 60 FPS como target óptimo.
- El vídeo se almacena localmente en el dispositivo antes de enviarse al motor de procesamiento.

#### CA‑01.02 — Importación desde galería
- El selector de archivos acepta formatos MP4, MOV y AVI.
- El sistema valida que el archivo no exceda 500 MB ni una duración de 5 minutos (límites configurables).
- Se muestra una previsualización del vídeo seleccionado antes de confirmar el procesamiento.

#### CA‑01.03 — Pipeline de visión por computadora
- El sistema ejecuta un modelo de estimación de pose (p. ej. pose‑2D/3D con salida de 17‑33 puntos clave corporales) sobre cada fotograma del vídeo.
- Se realiza seguimiento temporal (*tracking*) para mantener la identidad de la patinadora a lo largo del metraje.
- El pipeline extrae, al menos, los siguientes parámetros por fotograma:
  - Ángulos articulares (tobillo, rodilla, cadera, hombro, codo, muñeca).
  - Velocidad e inclinación del eje corporal.
  - Altura del centro de masas (despegue/aterrizaje en saltos).
  - Tasa de rotación (rev/s) durante saltos y piruetas.
  - Duración de posiciones sostenidas.
- El procesamiento se ejecuta en *background* y notifica el progreso (0‑100 %).

#### CA‑01.04 — Reconocimiento automático de elementos técnicos
El motor de clasificación debe detectar y etiquetar, como mínimo:

| Categoría | Elementos |
|---|---|
| **Saltos** | Axel (1A, 2A, 3A), Salchow (S), Toe Loop (T), Loop (Lo), Flip (F), Lutz (Lz), Euler (1Eu). Detección de número de revoluciones (single/doble/triple) y tipo de pie de despegue/aterrizaje. |
| **Piruetas** | Pirueta recta (USp), pirueta sentada (SSp), pirueta de ángel (CSp), pirueta del revés (LSp), pirueta combinada (CoSp), pirueta de cambio de pie (CCoSp). Detección de número de rotaciones, posición básica y cambios de posición. |
| **Secuencias de pasos** | Secuencia de pasos en recta (StSq), secuencia circular (CiSt), secuencia en serpentina (SlSt). Detección de patrón de pasos (twizzles, brackets, counters, rockers, Choctaws). |
| **Elementos adicionales** | Death spiral (espiral de la muerte) en parejas, lifts (elevaciones), twist lifts, throw jumps (saltos lanzados), choreographic sequences. |

- Cada elemento detectado se etiqueta con: tipo, timestamp de inicio/fin, nivel de dificultad estimado (B / 1 / 2 / 3 / 4), y confianza del modelo (0‑100 %).

#### CA‑01.05 — Feedback visual y edición
- Tras el procesamiento, el usuario puede reproducir el vídeo con una línea de tiempo donde los elementos detectados aparecen como *capas* codificadas por color sobre la imagen.
- El usuario puede añadir, eliminar o reclasificar manualmente cualquier elemento detectado.
- Toda corrección manual se envía como *feedback* para reentrenamiento futuro del modelo.

#### CA‑01.06 — Rendimiento y offline
- El procesamiento de un vídeo de 3 minutos no debe superar los 5 minutos en un dispositivo gama media‑alta (Snapdragon 8 series / Apple A15 equivalentes).
- El usuario puede optar por procesar en el dispositivo (on‑device) o descargar el vídeo a un servidor opcional para procesamiento remoto.
- La app funciona completamente offline para captura y procesamiento local; el envío a servidor es una capacidad adicional.

---

## Historia de Usuario #2: Motor de Evaluación y Puntuación World Skate (Rollart)

| Campo | Valor |
|---|---|
| **ID** | HV‑02 |
| **Título** | Motor de Evaluación y Puntuación World Skate |
| **Rol** | Patinadora / Entrenador |
| **Rama Épica** | Reglamento y Scoring |
| **Dependencias** | HV‑01 |
| **Prioridad** | Crítica (MVP) |
| **Estimación** | 18 SP |

### Declaración

> **Como** patinadora o entrenador,
> **quiero** que el sistema cruce los elementos detectados por la IA con la tabla oficial de Valores Base (Scale of Values — SOV) y el reglamento de World Skate (Rollart) para calcular automáticamente una puntuación técnica completa, incluyendo el Grado de Ejecución (GOE) y las deducciones reglamentarias,
> **para** disponer de una puntuación simulada fiable sin depender de un panel de jueces humano.

### Criterios de Aceptación

#### CA‑02.01 — Tabla SOV versionada e inyectable
- La tabla de Valores Base (SOV) para todas las categorías (singles femenino/masculino, parejas, danza, precisión, cuartetos, solo dance) está almacenada en un archivo de datos estructurados (JSON / YAML / SQLite).
- El sistema permite cambiar la versión del reglamento (p. ej. "World Skate 2025‑2026", "World Skate 2026‑2027") sin modificar el código fuente.
- Se incluye un mecanismo de actualización remota de la SOV desde un endpoint oficial o repositorio de confianza.

#### CA‑02.02 — Asignación de valor base
- Por cada elemento detectado en HV‑01, el motor consulta la SOV y asigna el valor base correspondiente según:
  - Tipo de elemento (salto, pirueta, paso, etc.).
  - Número de revoluciones (saltos).
  - Nivel de dificultad (B / 1 / 2 / 3 / 4).
  - Categoría de competición (novatos, junior, senior).
- Si un elemento no es reconocido en la SOV, se marca como *No Identificado* y se muestra una alerta al usuario.

#### CA‑02.03 — Cálculo del Grado de Ejecución (GOE, rango −5 a +5)
- El sistema evalúa automáticamente los factores cualitativos del elemento basándose en los parámetros cinemáticos extraídos:

| Factor GOE (+) | Indicador detectado por IA |
|---|---|
| Altura / distancia en saltos | Diferencia vertical del centro de masas en despegue‑aterrizaje |
| Buena rotación en el aire / pirueta | Velocidad angular sostenida y alineación del eje |
| Aterrizaje controlado | Ángulo de rodilla y cadera en el impacto, mínimo desplazamiento lateral |
| Posición lograda (piruetas) | Ángulos articulares dentro del rango reglamentario durante ≥2 revoluciones |
| Fluidez y cobertura del hielo/pista | Velocidad de desplazamiento y suave variación de heading |

| Factor GOE (−) | Indicador detectado por IA |
|---|---|
| Rotación insuficiente (≤¼ rev en hielo) | Ángulo de rotación al aterrizar > 90° de la posición ideal |
| Caída | Centro de masas por debajo del umbral de pie, detección de impacto |
| Paso fuera / two‑footed landing | Ambos pies en contacto con el suelo tras aterrizaje de salto |
| Duración insuficiente de pirueta | Menos de 3 revoluciones en la posición base |
| Mala calidad de posición | Ángulos articulares fuera del rango reglamentario |
| Violación de tiempo de programa | Duración total del vídeo fuera del límite reglamentario (±10 %) |

- El GOE se calcula como: `GOE_base = 0; por cada factor positivo/negativo presente, se suma/resta 1 punto` (hasta ±5).
- El factor GOE se multiplica por el valor del elemento según la tabla GOE de World Skate (p. ej. GOE +1 = +10 % de SOV, +2 = +20 %, etc.).

#### CA‑02.04 — Deducciones automáticas
- **Caídas:** −1.0 punto por cada caída detectada (saltos, piruetas, elementos).
- **Violaciones de tiempo:** −1.0 si el programa excede el tiempo máximo en más de 5 segundos.
- **Elementos repetidos:** si se detecta el mismo salto más veces de las permitidas por el reglamento, el valor del elemento extra se marca como *No Válido* (no suma).

#### CA‑02.05 — Puntuación combinada (TES + PCS simulada)
- El motor muestra:
  - **TES (Total Element Score):** suma de SOV × factor GOE de cada elemento válido, menos deducciones.
  - **PCS (Program Component Score):** el usuario puede introducir notas manuales (Skills, Transitions, Performance, Composition, Interpretation) o el sistema puede sugerir valores basales según el nivel detectado.
  - **Puntuación Total:** TES + PCS.
- Se muestra una tabla detallada con cada elemento, su SOV, GOE, valor final y cualquier deducción aplicada.

#### CA‑02.06 — Verificación reglamentaria
- El motor advierte si el programa no cumple requisitos obligatorios (p. ej. número mínimo de piruetas, saltos obligatorios en la segunda mitad, tipos de secuencia de pasos requeridos según categoría).
- Emite una advertencia textual ("Warning: Solo se permite un Axel doble en programa corto").

---

## Historia de Usuario #3: Perfil de Rendimiento y Evolución de la Patinadora

| Campo | Valor |
|---|---|
| **ID** | HV‑03 |
| **Título** | Perfil de Rendimiento y Evolución de la Patinadora |
| **Rol** | Patinadora / Entrenador |
| **Rama Épica** | Analíticas y Seguimiento |
| **Dependencias** | HV‑02 |
| **Prioridad** | Alta |
| **Estimación** | 13 SP |

### Declaración

> **Como** patinadora o entrenador,
> **quiero** visualizar un panel de rendimiento que agregue el historial de todas las rutinas analizadas, con desglose por elemento, puntuación total simulada y gráficas de evolución temporal de los componentes del programa (Skills, Transitions, Performance, Choreography),
> **para** identificar tendencias, fortalezas y debilidades, y tomar decisiones informadas sobre el entrenamiento.

### Criterios de Aceptación

#### CA‑03.01 — Panel de sesión individual
- Tras procesar un vídeo (HV‑01 + HV‑02), se muestra una pantalla de resultados con:
  - **Resumen ejecutivo:** puntuación total (TES + PCS), número de elementos, número de deducciones.
  - **Lista detallada de elementos:** cada fila muestra icono del elemento, nombre, nivel, SOV, GOE, puntuación final y estado (válido / no válido / no identificado).
  - **Reproductor de vídeo** con línea de tiempo interactiva; al tocar un elemento de la lista, el vídeo salta al instante exacto de ejecución.
  - **Desglose del GOE:** "rueda" visual (+ / −) que detalla qué factores contribuyeron positiva y negativamente al GOE de cada elemento.

#### CA‑03.02 — Perfil acumulado de la patinadora
- La app mantiene un perfil persistente por patinadora (nombre, categoría, club, nivel competitivo).
- Cada vídeo analizado se almacena como una *sesión* dentro del perfil.
- El perfil muestra:
  - **Evolución del TES y PCS** a lo largo del tiempo (gráfico de líneas con puntos de sesión).
  - **Tasa de acierto/fallo** de elementos específicos (p. ej. % de ejecución exitosa del Axel doble en las últimas 10 sesiones).
  - **Progresión de niveles:** qué elementos han subido de nivel (B → 1 → 2 → 3 → 4) entre sesiones.
  - **Promedio del GOE** por tipo de elemento (saltos, piruetas, pasos) en cada sesión.

#### CA‑03.03 — Analíticas gráficas de componentes
- Gráficos radiales (radar chart) que comparan los 5 componentes del programa:
  - **Skills** – basado en variedad y dificultad de elementos detectados.
  - **Transitions** – basado en la densidad de cambios de dirección, pasos y conectores entre elementos.
  - **Performance** – basado en GOE promedio, velocidad y cobertura de pista.
  - **Choreography** – basado en la distribución espacial de los elementos y la variedad de posiciones.
  - **Interpretation** – basado en la sincronía (si aplica, o entrada manual).
- El radar puede superponer dos sesiones para comparación visual (p. ej. "Sesión actual vs. mejor sesión histórica").

#### CA‑03.04 — Histórico y exportación
- El listado de sesiones se presenta en orden cronológico inverso, con filtros por fecha, tipo de programa (corto / libre) y categoría.
- El usuario puede exportar un informe en PDF de cualquier sesión o del perfil completo, que incluya tabla de elementos, puntuaciones y gráficos.
- Los datos de perfil y sesiones se almacenan localmente con opción de respaldo en la nube (cuenta opcional).

---

## Historia de Usuario #4 (Técnica): Configuración y Actualización del Reglamento

| Campo | Valor |
|---|---|
| **ID** | HV‑04 |
| **Título** | Configuración y actualización del reglamento |
| **Rol** | Desarrollador / Administrador |
| **Rama Épica** | Mantenimiento del sistema |
| **Dependencias** | HV‑02 |
| **Prioridad** | Media |
| **Estimación** | 8 SP |

### Declaración

> **Como** desarrollador o administrador del sistema,
> **quiero** poder modificar la tabla SOV, los factores GOE y las reglas de elegibilidad de elementos sin recompilar la aplicación, mediante un archivo de configuración remoto o empaquetado con la APK,
> **para** mantener la aplicación actualizada frente a los cambios anuales del reglamento World Skate sin necesidad de publicar una nueva versión en cada ciclo.

### Criterios de Aceptación

- El archivo de reglas (`rollart_rules.json`) contiene la SOV completa, factores GOE, límites de tiempo, y restricciones por categoría.
- La app comprueba al arrancar si existe una versión más reciente del archivo en una URL configurable; si es así, lo descarga y lo aplica en caliente.
- Si no hay conexión, usa la versión empaquetada con la APK.
- El formato del archivo está documentado (esquema JSON) para que cualquier miembro del equipo pueda editarlo.

---

## Historia de Usuario #5 (Técnica): Privacidad y Almacenamiento Local

| Campo | Valor |
|---|---|
| **ID** | HV‑05 |
| **Título** | Privacidad y almacenamiento local de vídeos |
| **Rol** | Patinadora / Entrenador |
| **Rama Épica** | Infraestructura |
| **Dependencias** | HV‑01 |
| **Prioridad** | Alta |
| **Estimación** | 5 SP |

### Declaración

> **Como** patinadora o entrenador,
> **quiero** que todos los vídeos y datos analizados se almacenen exclusivamente en el dispositivo, sin ser compartidos con terceros a menos que yo lo autorice explícitamente,
> **para** garantizar la privacidad de las sesiones de entrenamiento, especialmente cuando trabajo con menores de edad.

### Criterios de Aceptación

- Todos los vídeos y datos de perfil residen en almacenamiento local aislado (app sandbox).
- No se realiza ninguna transmisión de vídeo o datos biométricos a servidores externos sin consentimiento explícito del usuario (opt‑in).
- En caso de activar el respaldo en la nube, los vídeos se cifran antes de la transmisión (AES‑256).
- La app solicita permiso de cámara y almacenamiento en tiempo de ejecución (Android Runtime Permissions).
- Se incluye una pantalla de privacidad que explica qué datos se recogen y cómo se procesan.

---

## Historia de Usuario #6 (Técnica): Procesamiento Offline de Extremo a Extremo

| Campo | Valor |
|---|---|
| **ID** | HV‑06 |
| **Título** | Procesamiento offline de extremo a extremo |
| **Rol** | Patinadora / Entrenador |
| **Rama Épica** | Infraestructura |
| **Dependencias** | HV‑01, HV‑02 |
| **Prioridad** | Alta |
| **Estimación** | 8 SP |

### Declaración

> **Como** patinadora o entrenador,
> **quiero** poder grabar, procesar y obtener la puntuación completa sin conexión a internet,
> **para** utilizar la aplicación en cualquier pista de patinaje, incluso sin cobertura móvil o WiFi.

### Criterios de Aceptación

- Todo el pipeline de visión (modelo de pose, clasificador de elementos) se ejecuta on‑device mediante runtime de inferencia local.
- El motor de reglas (SOV, GOE, deducciones) se evalúa localmente con los datos empaquetados.
- La descarga de modelos y reglas actualizadas solo requiere conexión en el momento de la actualización; no es necesaria para el funcionamiento diario.
- El tamaño total de los modelos descargables no supera los 150 MB (individualmente el modelo de pose ≤ 50 MB, el clasificador ≤ 30 MB).

---

## Priorización y Roadmap Sugerido

```
Sprint 1 (MVP - Semanas 1-4)
├── HV‑01: Captación y procesamiento de vídeo (funcionalidad básica: grabación, pose 2D, detección de saltos y piruetas)
├── HV‑02: Motor de evaluación (SOV básica singles, GOE automático, TES)
└── HV‑06: Procesamiento offline (infraestructura on‑device)

Sprint 2 (Semanas 5-8)
├── HV‑01: Completar detección de todos los elementos (pasos, parejas, danza)
├── HV‑03: Perfil de rendimiento y evolución (v1.0)
└── HV‑05: Privacidad y almacenamiento local

Sprint 3 (Semanas 9-12)
├── HV‑02: Completar GOE fino, deducciones, PCS simulado
├── HV‑03: Analíticas gráficas, radar chart, exportación PDF
└── HV‑04: Sistema de reglas inyectable y actualización remota
```

---

*Documento generado como base de planificación. Las estimaciones (Story Points) son preliminares y deben refinarse tras el primer sprint de descubrimiento técnico.*
