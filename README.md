# sshApp - Remote Server Manager (KMP)

**sshApp** es una aplicación multiplataforma (Android, iOS, Desktop) desarrollada para el Trabajo de Fin de Grado (TFG). Su objetivo es permitir la gestión y monitorización remota de servidores mediante SSH, permitiendo visualizar métricas en tiempo real, gestionar procesos y acceder a una terminal integrada.

## 🏗️ Arquitectura del Sistema

El proyecto sigue los principios de **Clean Architecture** y está construido sobre **Kotlin Multiplatform (KMP)**. La lógica de negocio, la base de datos y la UI son compartidas en un ~90%.

```mermaid
graph TD
    subgraph UI_Layer [Capa de Presentación - Compose Multiplatform]
        UI[Componentes UI]
        VM[ScreenModels / ViewModels]
        Voyager[Voyager Navigation]
    end

    subgraph Domain_Layer [Capa de Dominio - commonMain]
        SshService[SshService Interface]
        Models[Modelos: Server, Metrics, Process]
    end

    subgraph Data_Layer [Capa de Datos]
        Repo[ServerRepository]
        SQLD[(SQLDelight DB)]
    end

    subgraph Platform_Implementations [Implementaciones Nativa - Expect/Actual]
        AndroidJVM[SSHJ + BouncyCastle]
        iOS[Network.framework Connectivity]
    end

    UI --> VM
    VM --> Voyager
    VM --> Repo
    Repo --> SQLD
    Repo --> SshService
    SshService -.-> AndroidJVM
    SshService -.-> iOS
```
