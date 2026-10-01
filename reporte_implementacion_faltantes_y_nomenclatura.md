# Reporte de Implementación: Detección Centralizada de Faltantes, Salida Manual y Corrección de Nomenclatura

Se completó la implementación integral en el branch aislado `feature/graphite-missing-data-manual-output`, cumpliendo al 100% las directrices funcionales y técnicas acordadas.

> [!NOTE]
> Todos los ejemplos y valores de cuentas bancarias mostrados en este documento y en los casos de prueba son **100% sintéticos y ficticios** (p. ej. `012180001111111111`, `012180002222222222`). No contienen datos sensibles ni productivos.
> No se realizó ningún push, merge a otras ramas ni despliegue remoto. Los cambios residen exclusivamente en el branch local `feature/graphite-missing-data-manual-output`.

---

## 1. Nomenclatura XML Corregida (Sin Duplicidad de Planta)

Se corrigieron las fábricas XML para eliminar el sufijo duplicado de la unidad de negocio en el nombre del archivo:

| Planta / BU | Nombre Anterior (Duplicado) | Nombre Actual (Corregido) |
| :--- | :--- | :--- |
| **09 (PN)** | `RPIEDRAS_busrel_<erpIdQad>_09_09.xml`<br>`RPIEDRAS_creditor_<erpIdQad>_09_09.xml` | `RPIEDRAS_busrel_<erpIdQad>_09.xml`<br>`RPIEDRAS_creditor_<erpIdQad>_09.xml` |
| **99 (PN99)** | `PN99_busrel_<erpIdQad>_99_99.xml`<br>`PN99_creditor_<erpIdQad>_99_99.xml` | `PN99_busrel_<erpIdQad>_99.xml`<br>`PN99_creditor_<erpIdQad>_99.xml` |
| **0111 / 0301 (OCBYP)** | `busrel_<erpIdQad>_0111_0111.xml`<br>`creditor_<erpIdQad>_0111_0111.xml` | `busrel_<erpIdQad>_0111.xml`<br>`creditor_<erpIdQad>_0111.xml` |
| **1000 (Frenos)** | `busrel_<erpIdQad>_1000_1000.xml`<br>`creditor_<erpIdQad>_1000_1000.xml` | `busrel_<erpIdQad>_1000.xml`<br>`creditor_<erpIdQad>_1000.xml` |
| **1850 (Breakes)** | `busrel_<erpIdQad>_1850_1850.xml`<br>`creditor_<erpIdQad>_1850_1850.xml` | `busrel_<erpIdQad>_1850.xml`<br>`creditor_<erpIdQad>_1850.xml` |

### Idempotencia Histórica
En `XmlGenerationHelper`, se valida si existe el archivo con el nuevo formato `_BU.xml` **o** si ya existía el archivo con el formato histórico duplicado `_BU_BU.xml`. En ambos casos se respeta la idempotencia sin regenerar ni sobreescribir.

---

## 2. Salida y Rutas por Tipo de Archivo y Severidad

### XMLs de QAD
- **Normales (Completos sin faltantes):**
  - `<output>/xml/PN/`
  - `<output>/xml/PN99/`
  - `<output>/xml/OCBYP/`
  - `<output>/xml/FRENOS/`
  - `<output>/xml/BREAKES/`
- **Con faltantes no bloqueantes (`WARNING`):**
  - `<output>/xml/Manual/PN/`
  - `<output>/xml/Manual/PN99/`
  - `<output>/xml/Manual/OCBYP/`
  - `<output>/xml/Manual/FRENOS/`
  - `<output>/xml/Manual/BREAKES/`
- **Con faltantes bloqueantes (`BLOCKING` / `CRITICAL`):**
  - No se genera archivo corrupto (`NOT_GENERATED`).
  - La fila se marca con `xmlStatus = ERROR` en base de datos.
- **Recuperación Manual a Normal:** Si un archivo fue generado en `Manual` y en una ejecución posterior se reciben los datos completos, el sistema genera el archivo en la carpeta normal sin quedar bloqueado por la versión previa en `Manual`.

### Archivos TXT de Integrity
- **Separación estricta por BU:** Cada unidad de negocio tiene su propia subcarpeta.
- **Normales (Completos):** `<output>/integrity/<BU>/<erpIdQad>_<timestamp>.txt`
  - Ejemplos: `<output>/integrity/09/`, `<output>/integrity/0111/`, etc.
- **Con datos faltantes no bloqueantes (`WARNING`):**
  - `<output>/integrity/Manual/<BU>/<erpIdQad>_<timestamp>.txt`
- **Cuentas bloqueantes:** Si una cuenta no tiene número de cuenta (`accountNumber`), se omite esa cuenta de la exportación y se reporta en el correo sin abortar las demás cuentas ni los demás proveedores.

---

## 3. Manejo de Nodos Alternos Disambiguados

Si un dato bancario (por ejemplo `Bank_Wire_ABA_Routing`) viene a nivel `ERP_Record` y no en `ERP_Bank_List`:
- Si el proveedor tiene **exactamente 1 cuenta bancaria**, el dato puede ser asociado y se registra como advertencia informativa.
- Si el proveedor tiene **múltiples cuentas bancarias**, el dato es ambiguo (`AMBIGUOUS_NODE`) y no se asigna ciegamente a ninguna cuenta. La generación para esa BU se redirige a `<output>/integrity/Manual/<BU>/` y se reporta detalladamente en el correo.

---

## 4. Notificación Consolidada Asíncrona (Sin Fallbacks ni Envíos Directos)

- Todas las incidencias detectadas en una ejecución se acumulan en memoria en el `MissingDataCollector`.
- Al finalizar el procesamiento del lote en `SupplierProcessingServiceImpl`, `MissingDataNotificationService` consolida las incidencias agrupadas por Proveedor y Planta.
- Los números de cuenta bancaria se enmascaran obligatoriamente (`****1234`), sin imprimir datos completos ni JSON crudo.
- Se inserta un registro en la tabla `correo_pendiente` (`CorreoPendienteEntity`) con `enviado = false`.
- **El scheduler existente (`CorreoPendienteScheduler`) se encarga de enviarlo en su ciclo de cron habitual**, respetando estrictamente la arquitectura desacoplada.

---

## 5. Resultados de Pruebas Automatizadas

Se ejecutó la suite completa con Maven (`mvn clean test` y `mvn clean package`):
```text
[INFO] Tests run: 38, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Tests verificados:
1. `MissingDataAndNomenclatureIntegrationTest` (10 pruebas):
   - Nomenclatura sin duplicidad en todas las BUs (09, 99, 0111, 0301, 1000, 1850).
   - Idempotencia con archivos históricos `_BU_BU.xml`.
   - Generación en `Manual/<BU>` ante advertencias no bloqueantes.
   - Supresión de generación corrupta ante faltantes bloqueantes.
   - Separación estricta de Integrity por BU (`<integrity>/<BU>/` y `<integrity>/Manual/<BU>/`).
   - Recuperación de Manual a normal tras corrección de datos.
   - Nodos alternos inequívocos vs ambiguos (`AMBIGUOUS_NODE`).
   - Aislamiento de proveedores en lote ante errores.
   - Persistencia de correo pendiente con enmascaramiento y `enviado=false`.
2. `Mx190235MultiAccountXmlTest` (2 pruebas):
   - Coexistencia de cuenta histórica y nueva para MX190235 sin `NonUniqueResultException`.
   - Generación de Modify/PartialUpdate para proveedores con historial en QAD.
   - Generación de Create/Save para proveedores nuevos.
3. `ErpResolutionConsistencyTest` (6 pruebas).
4. `SupplierProcessingServiceTest` (9 pruebas).
5. `SupplierErpResolverTest` (10 pruebas).
6. `IntegrityMigrationByErpIdTest` (1 prueba).
7. `GraphiteClientApplicationTests` (1 prueba).
