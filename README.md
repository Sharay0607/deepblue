# CourseHub — Student Lab

Este repositorio es el punto de partida del laboratorio.

## Regla principal

No copies la solución de otra persona. Antes de escribir código, dibuja el modelo y predice el SQL.

## Ejecutar

```bash
mvn test
```

Los ejercicios con `@Disabled` se irán habilitando durante el curso.

## Buscar tareas pendientes

```bash
grep -R "TODO-STUDENT" -n src
```

En Windows PowerShell:

```powershell
Get-ChildItem -Recurse src | Select-String "TODO-STUDENT"
```
