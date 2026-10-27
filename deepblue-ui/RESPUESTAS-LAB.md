# Laboratorio DeepBlue Rescue – Respuestas
Nombres: Sharay Machacon - Juan Bermudez
Fecha: 26/10/2026

## Para pensar

1. **¿Por qué el fetch usa `/api/...` y no `http://localhost:8080/api/...`?**
   Porque se utiliza el proxy de Vite configurado en `vite.config.js`. Esto evita problemas de CORS en desarrollo y permite que las peticiones se redirijan internamente al backend.

2. **¿Por qué `request` lanza el error en lugar de devolverlo?**
   Para que TanStack Query (tanto `useQuery` como `useMutation`) reconozca que la promesa fue rechazada y pueda gestionar los estados `isError` y `error` automáticamente.

3. **¿Qué pasaría si `queryKey` fuera solo `['rescue-cases']` sin `status`?**
   Todos los filtros compartirían la misma entrada en la caché. Al cambiar de estado en el selector, React Query entregaría los datos cacheados del filtro anterior sin consultar las listas actualizadas al servidor.

4. **¿Para qué sirve `enabled`? ¿Qué pasaría sin él si `animalCode` fuera `undefined`?**
   `enabled` condiciona la ejecución de la consulta. Sin él, TanStack Query intentaría consultar la URL `/api/animals/undefined`, lo cual dispararía peticiones innecesarias y errores HTTP 400/404 al backend.

5. **¿Qué hace `invalidateQueries({ queryKey: ['rescue-cases'] })` y por qué afecta a todas las listas por estado?**
   Invalida por coincidencia de prefijo todas las claves en caché que comiencen con `['rescue-cases']`. Como las claves por filtro son `['rescue-cases', status]`, todas las listas de todos los estados quedan marcadas como obsoletas y se vuelven a pedir al servidor.

6. **¿Por qué `useReducer` y no un `useState` por campo? ¿Por qué `invalidateQueries` usa `['animal', animalCode]` sin más sufijos?**
   - `useReducer` permite agrupar y gestionar el estado del formulario en un único objeto mediante acciones centralizadas (`field`, `reset`), evitando múltiples manejadores de `useState`.
   - La invalidación con `['animal', animalCode]` aprovecha la jerarquía por prefijo de TanStack Query para invalidar simultáneamente las tres subconsultas del animal: datos básicos, lista de tratamientos y elegibilidad.

## Observaciones de las pruebas

### Paso 5 (cambio de estado)
- **ADMITTED → UNDER_EVALUATION:** Transición exitosa. El caso cambia de lista sin recargar la página.
- **ADMITTED → RELEASED:** Error 409 capturado y mostrado en el componente `ErrorMessage`.

### Paso 6 (registro de tratamiento)
- **Descripción corta:** Devuelve error HTTP 400 especificando la regla de longitud (entre 10 y 500 caracteres).
- **specialistCode vacío:** Devuelve error HTTP 400 indicando campo requerido.
- **Fecha en el futuro:** Devuelve error HTTP 400 impidiendo registro de fechas futuras.
- **Fecha vacía:** Devuelve error HTTP 400.
- **Especialista SP-999:** Devuelve error HTTP 404 notificando que el especialista no existe.
- **Datos válidos:** Retorna HTTP 201, limpia el formulario con la acción `reset` del reducer y refresca la tabla de tratamientos al invalidar la caché del animal.