import './App.css';
import { AuthenticatedTemplate, UnauthenticatedTemplate, useMsal } from '@azure/msal-react';
import { loginRequest, apiRequest } from './authConfig';
import { useCallback, useEffect, useState } from 'react';
import Axios from 'axios';


function App() {

  // Estados de la consulta backend
  const { instance, accounts } = useMsal();
  const [usuarioBackend, setUsuarioBackend] = useState(null);
  const [errorBackend, setErrorBackend] = useState(null);
  // Agregado
  const [cargando, setCargando] = useState(false);

  //Estados del formulario
  // Agregado
  const [titulo, setTitulo] = useState("");
  // Agregado
  const [descripcion, setDescripcion] = useState("");
  // Agregado
  const [prioridad, setPrioridad] = useState("");
  // Agregado
  const [errorSolicitud, setErrorSolicitud] = useState("");
  // Agregado , mostraremos el resumen en la pagina
  const [solicitudRevisada, setSolicitudRevisada] = useState(null);

  // Agregados funcionales con la api
  const [enviandoSolicitud, setEnviandoSolicitud] = useState(false);
  const [solicitudCreada, setSolicitudCreada] = useState(null);
  const [solicitudes, setSolicitudes] = useState([]);

  // Estados del catálogo
  const [categorias, setCategorias] = useState([]);
  // Copia local de categorías en Solicitudes (recibidas por RabbitMQ)
  const [categoriasLocales, setCategoriasLocales] = useState([]);
  const [errorCategoriasLocales, setErrorCategoriasLocales] = useState("");
  const [ultimaRevisionLocal, setUltimaRevisionLocal] = useState(null);
  const [categoriaId, setCategoriaId] = useState("");
  const [errorCatalogo, setErrorCatalogo] = useState("");

  // Estados para administrar el catalogo
  const [nombreCategoria, setNombreCategoria] = useState("");
  const [descripcionCategoria, setDescripcionCategoria] = useState("");
  const [categoriaEditandoId, setCategoriaEditandoId] = useState(null);
  const [procesandoCategoria, setProcesandoCategoria] = useState(false);
  const [mensajeCatalogo, setMensajeCatalogo] = useState("");

  // Estados para administrar prioridades
  const [prioridades, setPrioridades] = useState([]);
  const [nombrePrioridad, setNombrePrioridad] = useState("");
  const [descripcionPrioridad, setDescripcionPrioridad] = useState("");
  const [prioridadEditandoId, setPrioridadEditandoId] = useState(null);
  const [procesandoPrioridad, setProcesandoPrioridad] = useState(false);
  const [mensajePrioridades, setMensajePrioridades] = useState("");
  const [errorPrioridades, setErrorPrioridades] = useState("");

  // Agregados para actualizar mediante patch
  const [solicitudActualizandoId, setSolicitudActualizandoId] = useState(null);
  const [mensajeEstado, setMensajeEstado] = useState("");
  const [errorEstado, setErrorEstado] = useState("");

  const esGestorSolicitudes =
    usuarioBackend?.roles?.includes("ROLE_OPERADOR") ||
    usuarioBackend?.roles?.includes("ROLE_ADMINISTRADOR");

  const esCliente =
    usuarioBackend?.roles?.includes("ROLE_CLIENTE");

  const esAdministrador =
    usuarioBackend?.roles?.includes("ROLE_ADMINISTRADOR");

  const obtenerEstadosPermitidos = (estadoActual) => {
    switch (estadoActual) {
      case "CREADA":
        return ["ASIGNADA", "CANCELADA"];

      case "ASIGNADA":
        return ["EN_PROCESO", "CANCELADA"];

      case "EN_PROCESO":
        return ["RESUELTA", "CANCELADA"];

      case "RESUELTA":
        return ["CERRADA"];

      case "CERRADA":
      case "CANCELADA":
        return [];

      default:
        return [];
    }
  };

  const mostrarEstado = (estado) => {
    switch (estado) {
      case "CREADA":
        return "Creada";

      case "ASIGNADA":
        return "Asignada";

      case "EN_PROCESO":
        return "En proceso";

      case "RESUELTA":
        return "Resuelta";

      case "CERRADA":
        return "Cerrada";

      case "CANCELADA":
        return "Cancelada";

      default:
        return estado;
    }
  };


  const iniciarSesion = () => {
    instance.loginRedirect(loginRequest)
      .catch(error => {
        console.error(error);
      });
  }

  const cerrarSesion = () => {
    instance.logoutRedirect();
  }

  useEffect(() => {
    if (accounts.length === 0) {
      return;
    }


    const obtenerUsuarioBackend = async () => {
      setCargando(true);
      setErrorBackend(null);
      setUsuarioBackend(null);
      setErrorCatalogo("");
      setErrorPrioridades("");

      try {
        const respuestaToken = await instance.acquireTokenSilent({
          ...apiRequest,
          account: accounts[0]
        });

        const configuracion = {
          headers: {
            Authorization: `Bearer ${respuestaToken.accessToken}`
          }
        };

        const respuestaUsuario = await Axios.get(
          `${process.env.REACT_APP_API_BASE_URL}/v2/usuario`,
          configuracion
        );

        const rolesUsuario = respuestaUsuario.data.roles || [];

        const puedeGestionarSolicitudes =
          rolesUsuario.includes("ROLE_OPERADOR") ||
          rolesUsuario.includes("ROLE_ADMINISTRADOR");

        const rutaSolicitudes = puedeGestionarSolicitudes
          ? "/v2/solicitudes"
          : "/v2/solicitudes/mias";

        const respuestaSolicitudes = await Axios.get(
          `${process.env.REACT_APP_API_BASE_URL}${rutaSolicitudes}`,
          configuracion
        );

        setUsuarioBackend(respuestaUsuario.data);
        setSolicitudes(respuestaSolicitudes.data);
        try {
          const respuestaCatalogo = await Axios.get(
            `${process.env.REACT_APP_API_BASE_URL}/v2/catalogo`,
            configuracion
          );

          setCategorias(respuestaCatalogo.data);
        } catch (errorCatalogoRespuesta) {
          console.error(errorCatalogoRespuesta);
          setCategorias([]);
          setErrorCatalogo(
            "No fue posible cargar las categorías."
          );
        }

        try {
          const respuestaLocales = await Axios.get(
            `${process.env.REACT_APP_API_BASE_URL}/v2/categorias-locales`,
            configuracion
          );

          setCategoriasLocales(respuestaLocales.data);
          setUltimaRevisionLocal(new Date());
        } catch (errorLocalesRespuesta) {
          console.error(errorLocalesRespuesta);
          setCategoriasLocales([]);
          setErrorCategoriasLocales(
            "No fue posible cargar las categorías disponibles en Solicitudes."
          );
        }

        try {
          const respuestaPrioridades = await Axios.get(
            `${process.env.REACT_APP_API_BASE_URL}/v2/catalogo/prioridades`,
            configuracion
          );

          setPrioridades(respuestaPrioridades.data);
        } catch (errorPrioridadesRespuesta) {
          console.error(errorPrioridadesRespuesta);
          setPrioridades([]);
          setErrorPrioridades(
            "No fue posible cargar las prioridades."
          );
        }

      } catch (error) {
        console.error(error);
        setErrorBackend(
          "No fue posible consultar tus datos. Inténtalo nuevamente."
        );
      } finally {
        setCargando(false);
      }
    };

    obtenerUsuarioBackend();
  }, [accounts, instance]);

  //  ----------------- Funcion para REVISAR la solicitud -----------------
  const revisarSolicitud = () => {
    setErrorSolicitud("");
    setSolicitudRevisada(null);

    if (
      titulo.trim() === "" ||
      descripcion.trim() === "" ||
      categoriaId === "" ||
      prioridad === ""
    ) {
      setErrorSolicitud(
        "Los campos título, descripción, categoría y prioridad son obligatorios."
      );
      return;
    }

    const categoriaSeleccionada = categoriasLocales.find(
      (categoria) => String(categoria.categoriaId) === categoriaId
    );

    if (!categoriaSeleccionada) {
      setErrorSolicitud(
        "La categoría seleccionada todavía no está disponible en Solicitudes."
      );
      return;
    }

    const prioridadSeleccionada = prioridades.find(
      (prioridadCatalogo) =>
        prioridadCatalogo.nombre === prioridad
    );

    if (!prioridadSeleccionada) {
      setErrorSolicitud(
        "La prioridad seleccionada no es válida."
      );
      return;
    }

    setSolicitudRevisada({
      titulo: titulo.trim(),
      descripcion: descripcion.trim(),
      categoriaId: Number(categoriaId),
      categoriaNombre: categoriaSeleccionada.nombre,
      prioridad: prioridadSeleccionada.nombre
    });
  };

  //  ----------------- Funcion asincrona para ENVIAR la solicitud -----------------
  const enviarSolicitud = async () => {
    if (!solicitudRevisada || accounts.length === 0) {
      return;
    }

    setEnviandoSolicitud(true);
    setErrorSolicitud("");

    try {
      const respuestaToken = await instance.acquireTokenSilent({
        ...apiRequest,
        account: accounts[0]
      });

      const respuesta = await Axios.post(
        `${process.env.REACT_APP_API_BASE_URL}/v2/solicitudes`,
        solicitudRevisada,
        {
          headers: {
            Authorization: `Bearer ${respuestaToken.accessToken}`,
            "Content-Type": "application/json"
          }
        }
      );

      setSolicitudCreada(respuesta.data);
      setSolicitudes((solicitudesAnteriores) => [
        respuesta.data,
        ...solicitudesAnteriores
      ]);
      setSolicitudRevisada(null);
      setTitulo("");
      setDescripcion("");
      setCategoriaId("");
      setPrioridad("");
    } catch (error) {
      console.error(error);
      setErrorSolicitud(
        error.response?.data?.mensaje || "No fue posible crear la solicitud."
      );
    } finally {
      setEnviandoSolicitud(false);
    }
  };

  //  ----------------- Funcion asincrona para ACTUALIZAR estado -----------------
  const actualizarEstado = async (id, nuevoEstado) => {
    if (accounts.length === 0) {
      return;
    }

    setSolicitudActualizandoId(id);
    setMensajeEstado("");
    setErrorEstado("");

    try {
      const respuestaToken = await instance.acquireTokenSilent({
        ...apiRequest,
        account: accounts[0]
      });

      const respuesta = await Axios.patch(
        `${process.env.REACT_APP_API_BASE_URL}/v2/solicitudes/${id}/estado`,
        {
          estado: nuevoEstado
        },
        {
          headers: {
            Authorization: `Bearer ${respuestaToken.accessToken}`,
            "Content-Type": "application/json"
          }
        }
      );

      setSolicitudes((solicitudesAnteriores) =>
        solicitudesAnteriores.map((solicitud) =>
          solicitud.id === id ? respuesta.data : solicitud
        )
      );

      setMensajeEstado(
        `Estado de la solicitud ${id} actualizado correctamente.`
      );
    } catch (error) {
      console.error(error);

      const mensajeError =
        error.response?.data?.mensaje ||
        `No fue posible actualizar la solicitud ${id}.`;

      setErrorEstado(mensajeError);
    } finally {
      setSolicitudActualizandoId(null);
    }
  };

  //  ----------------- Funcion asincrona de configuración autorizada para el catálogo -----------------
  const obtenerConfiguracionAutorizada = async () => {
    const respuestaToken = await instance.acquireTokenSilent({
      ...apiRequest,
      account: accounts[0]
    });

    return {
      headers: {
        Authorization: `Bearer ${respuestaToken.accessToken}`,
        "Content-Type": "application/json"
      }
    };
  };

  //  ----------------- Recargar la copia local de categorías (Solicitudes) -----------------
  const recargarCategoriasLocales = useCallback(async () => {
    if (accounts.length === 0) {
      return;
    }

    try {
      const respuestaToken = await instance.acquireTokenSilent({
        ...apiRequest,
        account: accounts[0]
      });

      const respuesta = await Axios.get(
        `${process.env.REACT_APP_API_BASE_URL}/v2/categorias-locales`,
        {
          headers: {
            Authorization: `Bearer ${respuestaToken.accessToken}`
          }
        }
      );

      setCategoriasLocales(respuesta.data);
      setErrorCategoriasLocales("");
      setUltimaRevisionLocal(new Date());
    } catch (error) {
      console.error(error);
      setErrorCategoriasLocales(
        "No fue posible consultar las categorías disponibles en Solicitudes."
      );
    }
  }, [accounts, instance]);

  //  ----------------- Recargar las categorías originales (Catálogo) -----------------
  const recargarCategoriasCatalogo = async () => {
    try {
      const configuracion = await obtenerConfiguracionAutorizada();
      const respuesta = await Axios.get(
        `${process.env.REACT_APP_API_BASE_URL}/v2/catalogo`,
        configuracion
      );
      setCategorias(respuesta.data);
    } catch (error) {
      console.error(error);
    }
  };

  // ¿La categoría del catálogo ya llegó a Solicitudes?
  const estaDisponibleEnSolicitudes = (categoriaIdCatalogo) =>
    categoriasLocales.some(
      (categoriaLocal) => categoriaLocal.categoriaId === categoriaIdCatalogo
    );

  const hayCategoriasPendientes = categorias.some(
    (categoria) => !estaDisponibleEnSolicitudes(categoria.id)
  );

  // Mientras existan categorías pendientes, revisar cada 5 segundos (procesamiento asíncrono)
  useEffect(() => {
    if (!esAdministrador || !hayCategoriasPendientes) {
      return;
    }

    const intervalo = setInterval(recargarCategoriasLocales, 5000);
    return () => clearInterval(intervalo);
  }, [esAdministrador, hayCategoriasPendientes, recargarCategoriasLocales]);

  //  ----------------- Funcion asincrona de crear o actualizar una categoría -----------------
  const guardarCategoria = async () => {
    if (accounts.length === 0) {
      return;
    }

    if (
      nombreCategoria.trim() === "" ||
      descripcionCategoria.trim() === ""
    ) {
      setErrorCatalogo(
        "El nombre y la descripción de la categoría son obligatorios."
      );
      return;
    }

    setProcesandoCategoria(true);
    setMensajeCatalogo("");
    setErrorCatalogo("");

    try {
      const configuracion =
        await obtenerConfiguracionAutorizada();

      const peticion = {
        nombre: nombreCategoria.trim(),
        descripcion: descripcionCategoria.trim()
      };

      if (categoriaEditandoId === null) {
        const respuesta = await Axios.post(
          `${process.env.REACT_APP_API_BASE_URL}/v2/catalogo`,
          peticion,
          configuracion
        );

        setCategorias((categoriasAnteriores) => [
          ...categoriasAnteriores,
          respuesta.data
        ]);

        setMensajeCatalogo(
          `Categoría ${respuesta.data.id} guardada en Catálogo y evento ${respuesta.data.eventId} confirmado por RabbitMQ. ` +
          "Aparecerá como disponible cuando Solicitudes procese el evento."
        );
      } else {
        const respuesta = await Axios.put(
          `${process.env.REACT_APP_API_BASE_URL}/v2/catalogo/${categoriaEditandoId}`,
          peticion,
          configuracion
        );

        setCategorias((categoriasAnteriores) =>
          categoriasAnteriores.map((categoria) =>
            categoria.id === categoriaEditandoId
              ? respuesta.data
              : categoria
          )
        );

        setMensajeCatalogo(
          "Categoría actualizada correctamente."
        );
      }

      setNombreCategoria("");
      setDescripcionCategoria("");
      setCategoriaEditandoId(null);
    } catch (error) {
      console.error(error);

      if (error.response?.status === 503 && error.response?.data?.categoriaId) {
        // La categoría quedó guardada, pero el evento no fue confirmado por RabbitMQ
        setErrorCatalogo(error.response.data.mensaje);
        await recargarCategoriasCatalogo();
      } else {
        setErrorCatalogo(
          error.response?.data?.mensaje || "No fue posible guardar la categoría."
        );
      }
    } finally {
      setProcesandoCategoria(false);
    }
  };

  //  ----------------- Funcion que Coloca una categoría existente dentro del formulario -----------------
  const prepararEdicionCategoria = (categoria) => {
    setCategoriaEditandoId(categoria.id);
    setNombreCategoria(categoria.nombre);
    setDescripcionCategoria(categoria.descripcion);
    setMensajeCatalogo("");
    setErrorCatalogo("");
  };

  //  ----------------- Funcion que cancela la edición y vacía el formulario -----------------
  const cancelarEdicionCategoria = () => {
    setCategoriaEditandoId(null);
    setNombreCategoria("");
    setDescripcionCategoria("");
    setMensajeCatalogo("");
    setErrorCatalogo("");
  };

  //  ----------------- Funcion que Elimina una categoría -----------------
  const eliminarCategoria = async (categoria) => {
    const confirmacion = window.confirm(
      `¿Deseas eliminar la categoría ${categoria.nombre}?`
    );

    if (!confirmacion || accounts.length === 0) {
      return;
    }

    setProcesandoCategoria(true);
    setMensajeCatalogo("");
    setErrorCatalogo("");

    try {
      const configuracion =
        await obtenerConfiguracionAutorizada();

      await Axios.delete(
        `${process.env.REACT_APP_API_BASE_URL}/v2/catalogo/${categoria.id}`,
        configuracion
      );

      setCategorias((categoriasAnteriores) =>
        categoriasAnteriores.filter(
          (categoriaActual) =>
            categoriaActual.id !== categoria.id
        )
      );

      if (categoriaEditandoId === categoria.id) {
        setCategoriaEditandoId(null);
        setNombreCategoria("");
        setDescripcionCategoria("");
      }

      setMensajeCatalogo(
        "Categoría eliminada correctamente."
      );
    } catch (error) {
      console.error(error);
      setErrorCatalogo(
        "No fue posible eliminar la categoría."
      );
    } finally {
      setProcesandoCategoria(false);
    }
  };

  //  ----------------- Función para crear o actualizar una prioridad -----------------
  const guardarPrioridad = async () => {
    if (accounts.length === 0) {
      return;
    }

    if (nombrePrioridad.trim() === "") {
      setErrorPrioridades(
        "El nombre de la prioridad es obligatorio."
      );
      return;
    }

    setProcesandoPrioridad(true);
    setMensajePrioridades("");
    setErrorPrioridades("");

    try {
      const configuracion =
        await obtenerConfiguracionAutorizada();

      const peticion = {
        nombre: nombrePrioridad.trim().toUpperCase(),
        descripcion: descripcionPrioridad.trim()
      };

      if (prioridadEditandoId === null) {
        const respuesta = await Axios.post(
          `${process.env.REACT_APP_API_BASE_URL}/v2/catalogo/prioridades`,
          peticion,
          configuracion
        );

        setPrioridades((prioridadesAnteriores) => [
          ...prioridadesAnteriores,
          respuesta.data
        ]);

        setMensajePrioridades(
          "Prioridad creada correctamente."
        );
      } else {
        const respuesta = await Axios.put(
          `${process.env.REACT_APP_API_BASE_URL}/v2/catalogo/prioridades/${prioridadEditandoId}`,
          peticion,
          configuracion
        );

        setPrioridades((prioridadesAnteriores) =>
          prioridadesAnteriores.map((prioridadActual) =>
            prioridadActual.id === prioridadEditandoId
              ? respuesta.data
              : prioridadActual
          )
        );

        setMensajePrioridades(
          "Prioridad actualizada correctamente."
        );
      }

      setNombrePrioridad("");
      setDescripcionPrioridad("");
      setPrioridadEditandoId(null);
    } catch (error) {
      console.error(error);
      setErrorPrioridades(
        "No fue posible guardar la prioridad."
      );
    } finally {
      setProcesandoPrioridad(false);
    }
  };

  //  ----------------- Función para preparar la edición de una prioridad -----------------
  const prepararEdicionPrioridad = (prioridadCatalogo) => {
    setPrioridadEditandoId(prioridadCatalogo.id);
    setNombrePrioridad(prioridadCatalogo.nombre);
    setDescripcionPrioridad(
      prioridadCatalogo.descripcion || ""
    );
    setMensajePrioridades("");
    setErrorPrioridades("");
  };

  //  ----------------- Función para cancelar la edición de una prioridad -----------------
  const cancelarEdicionPrioridad = () => {
    setPrioridadEditandoId(null);
    setNombrePrioridad("");
    setDescripcionPrioridad("");
    setMensajePrioridades("");
    setErrorPrioridades("");
  };

  //  ----------------- Función para eliminar una prioridad -----------------
  const eliminarPrioridad = async (prioridadCatalogo) => {
    const confirmacion = window.confirm(
      `¿Deseas eliminar la prioridad ${prioridadCatalogo.nombre}?`
    );

    if (!confirmacion || accounts.length === 0) {
      return;
    }

    setProcesandoPrioridad(true);
    setMensajePrioridades("");
    setErrorPrioridades("");

    try {
      const configuracion =
        await obtenerConfiguracionAutorizada();

      await Axios.delete(
        `${process.env.REACT_APP_API_BASE_URL}/v2/catalogo/prioridades/${prioridadCatalogo.id}`,
        configuracion
      );

      setPrioridades((prioridadesAnteriores) =>
        prioridadesAnteriores.filter(
          (prioridadActual) =>
            prioridadActual.id !== prioridadCatalogo.id
        )
      );

      if (prioridadEditandoId === prioridadCatalogo.id) {
        setPrioridadEditandoId(null);
        setNombrePrioridad("");
        setDescripcionPrioridad("");
      }

      setMensajePrioridades(
        "Prioridad eliminada correctamente."
      );
    } catch (error) {
      console.error(error);
      setErrorPrioridades(
        "No fue posible eliminar la prioridad."
      );
    } finally {
      setProcesandoPrioridad(false);
    }
  };

  //  ----------------- Funcion para LIMPIAR el formulario -----------------
  const limpiarFormulario = () => {
    setTitulo("");
    setDescripcion("");
    setCategoriaId("");
    setPrioridad("");
    setErrorSolicitud("");
    setSolicitudRevisada(null);
  };

  return (
    <div className="container" style={{ padding: "30px" }}>
      <h1>Login con Microsoft Entra ID</h1>
      <UnauthenticatedTemplate>
        <p className="alert alert-danger mt-3">
          El usuario no está autenticado.
        </p>
        <button onClick={iniciarSesion} className="btn btn-primary">
          Iniciar sesión
        </button>
      </UnauthenticatedTemplate>


      <AuthenticatedTemplate>

        {cargando && (
          <p className="alert alert-info">
            Consultando backend…
          </p>
        )}

        <h2>Usuario autenticado</h2>

        {usuarioBackend && (
          <div className="alert alert-success">
            <h2>Respuesta del backend</h2>
            <p>{usuarioBackend.mensaje}</p>
            <p>Nombre: {usuarioBackend.nombre}</p>
            <p>Usuario: {usuarioBackend.usuario}</p>
            <p>Roles: {usuarioBackend.roles?.join(", ") || "Sin rol asignado"}</p> {/* ? ejecuta joins solo si existen roles*/}
          </div>
        )}

        {errorBackend && (
          <div className="alert alert-danger">
            {errorBackend}
          </div>
        )}

        {/* ---------- TARJETA ADMINISTRACIÓN DEL CATÁLOGO ---------- */}
        {esAdministrador && (
          <div className="card p-3 mb-3">
            <h2>Administración del catálogo</h2>

            {mensajeCatalogo && (
              <div className="alert alert-success">
                {mensajeCatalogo}
              </div>
            )}

            {errorCatalogo && (
              <div className="alert alert-danger">
                {errorCatalogo}
              </div>
            )}

            <div className="mb-3">
              <label htmlFor="nombreCategoria" className="form-label">
                Nombre
              </label>

              <input
                id="nombreCategoria"
                type="text"
                className="form-control"
                value={nombreCategoria}
                onChange={(evento) =>
                  setNombreCategoria(evento.target.value)
                }
              />
            </div>

            <div className="mb-3">
              <label htmlFor="descripcionCategoria" className="form-label">
                Descripción
              </label>

              <textarea
                id="descripcionCategoria"
                className="form-control"
                rows={3}
                value={descripcionCategoria}
                onChange={(evento) =>
                  setDescripcionCategoria(evento.target.value)
                }
              />
            </div>

            <div className="mb-3">
              <button
                type="button"
                className="btn btn-primary me-2"
                onClick={guardarCategoria}
                disabled={procesandoCategoria}
              >
                {procesandoCategoria
                  ? "Guardando..."
                  : categoriaEditandoId === null
                    ? "Crear categoría"
                    : "Guardar cambios"}
              </button>

              {categoriaEditandoId !== null && (
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={cancelarEdicionCategoria}
                  disabled={procesandoCategoria}
                >
                  Cancelar edición
                </button>
              )}
            </div>

            <div className="d-flex align-items-center justify-content-between">
              <h3>Categorías registradas</h3>
              <button
                type="button"
                className="btn btn-outline-secondary btn-sm"
                onClick={recargarCategoriasLocales}
              >
                Verificar disponibilidad en Solicitudes
              </button>
            </div>

            <p className="form-text">
              "Disponible" indica que Solicitudes ya consumió el evento y guardó la copia en su RDS.
              {ultimaRevisionLocal &&
                ` Última revisión: ${ultimaRevisionLocal.toLocaleTimeString()}.`}
              {hayCategoriasPendientes && " Revisando automáticamente cada 5 s…"}
            </p>

            {errorCategoriasLocales && (
              <div className="alert alert-warning">
                {errorCategoriasLocales}
              </div>
            )}

            {categorias.length === 0 ? (
              <p>No hay categorías registradas.</p>
            ) : (
              <div className="table-responsive">
                <table className="table table-striped">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Nombre</th>
                      <th>Descripción</th>
                      <th>En Solicitudes</th>
                      <th>Acciones</th>
                    </tr>
                  </thead>

                  <tbody>
                    {categorias.map((categoria) => (
                      <tr key={categoria.id}>
                        <td>{categoria.id}</td>
                        <td>{categoria.nombre}</td>
                        <td>{categoria.descripcion}</td>
                        <td>
                          {estaDisponibleEnSolicitudes(categoria.id) ? (
                            <span className="badge bg-success">Disponible</span>
                          ) : (
                            <span className="badge bg-warning text-dark">Pendiente</span>
                          )}
                        </td>
                        <td>
                          <button
                            type="button"
                            className="btn btn-warning btn-sm me-2"
                            onClick={() =>
                              prepararEdicionCategoria(categoria)
                            }
                            disabled={procesandoCategoria}
                          >
                            Editar
                          </button>

                          <button
                            type="button"
                            className="btn btn-danger btn-sm"
                            onClick={() =>
                              eliminarCategoria(categoria)
                            }
                            disabled={procesandoCategoria}
                          >
                            Eliminar
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {/* ---------- TARJETA ADMINISTRACIÓN DE PRIORIDADES ---------- */}
        {esAdministrador && (
          <div className="card p-3 mb-3">
            <h2>Administración de prioridades</h2>

            {mensajePrioridades && (
              <div className="alert alert-success">
                {mensajePrioridades}
              </div>
            )}

            {errorPrioridades && (
              <div className="alert alert-danger">
                {errorPrioridades}
              </div>
            )}

            <div className="mb-3">
              <label
                htmlFor="nombrePrioridad"
                className="form-label"
              >
                Nombre
              </label>

              <input
                id="nombrePrioridad"
                type="text"
                className="form-control"
                value={nombrePrioridad}
                onChange={(evento) =>
                  setNombrePrioridad(evento.target.value)
                }
              />
            </div>

            <div className="mb-3">
              <label
                htmlFor="descripcionPrioridad"
                className="form-label"
              >
                Descripción
              </label>

              <textarea
                id="descripcionPrioridad"
                className="form-control"
                rows={3}
                value={descripcionPrioridad}
                onChange={(evento) =>
                  setDescripcionPrioridad(evento.target.value)
                }
              />
            </div>

            <div className="mb-3">
              <button
                type="button"
                className="btn btn-primary me-2"
                onClick={guardarPrioridad}
                disabled={procesandoPrioridad}
              >
                {procesandoPrioridad
                  ? "Guardando..."
                  : prioridadEditandoId === null
                    ? "Crear prioridad"
                    : "Guardar cambios"}
              </button>

              {prioridadEditandoId !== null && (
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={cancelarEdicionPrioridad}
                  disabled={procesandoPrioridad}
                >
                  Cancelar edición
                </button>
              )}
            </div>

            <h3>Prioridades registradas</h3>

            {prioridades.length === 0 ? (
              <p>No hay prioridades registradas.</p>
            ) : (
              <div className="table-responsive">
                <table className="table table-striped">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Nombre</th>
                      <th>Descripción</th>
                      <th>Acciones</th>
                    </tr>
                  </thead>

                  <tbody>
                    {prioridades.map((prioridadCatalogo) => (
                      <tr key={prioridadCatalogo.id}>
                        <td>{prioridadCatalogo.id}</td>
                        <td>{prioridadCatalogo.nombre}</td>
                        <td>
                          {prioridadCatalogo.descripcion ||
                            "Sin descripción"}
                        </td>
                        <td>
                          <button
                            type="button"
                            className="btn btn-warning btn-sm me-2"
                            onClick={() =>
                              prepararEdicionPrioridad(
                                prioridadCatalogo
                              )
                            }
                            disabled={procesandoPrioridad}
                          >
                            Editar
                          </button>

                          <button
                            type="button"
                            className="btn btn-danger btn-sm"
                            onClick={() =>
                              eliminarPrioridad(
                                prioridadCatalogo
                              )
                            }
                            disabled={procesandoPrioridad}
                          >
                            Eliminar
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {/*  ----------------- TARJETA PARA NUEVA SOLICITUD  ----------------- */}
        {/*Recuerden que el mb es margin bottom , y mt es margin top (Margen de arriba o abajo) */}
        <div className="card p-3 mb-3"
          style={{ display: esCliente ? "block" : "none" }}> {/* Div para el recuadro de solicitud */}

          <h2>Nueva solicitud</h2>

          <div className="mb-3">  {/* Titulo */}
            <label htmlFor="titulo" className="form-label"> {/* El form label es solo el estilo */}
              Título
            </label>
            <input
              id="titulo"
              type="text"
              className="form-control"
              value={titulo}
              onChange={(evento) => setTitulo(evento.target.value)}
            />
          </div>

          <div className="mb-3"> {/* Descripción */}
            <label htmlFor="descripcion" className="form-label">
              Descripción
            </label>
            <textarea
              id="descripcion"
              className="form-control"
              rows={3}
              value={descripcion}
              onChange={(evento) => setDescripcion(evento.target.value)}
            />
          </div>

          <div className="mb-3"> {/* Categoria */}
            {/* form-label: estilo de etiqueta */}
            <label htmlFor="categoria" className="form-label">
              Categoría
            </label>

            {/* form-select: estilo del selector */}
            <select
              id="categoria"
              className="form-select"
              value={categoriaId}
              onChange={(evento) => setCategoriaId(evento.target.value)}
              disabled={categoriasLocales.length === 0}
              aria-describedby="ayudaCategoria"
            >
              <option value="">Selecciona una categoría</option>

              {categoriasLocales.map((categoria) => (
                <option key={categoria.categoriaId} value={categoria.categoriaId}>
                  {categoria.nombre}
                </option>
              ))}
            </select>

            <div id="ayudaCategoria" className="form-text">
              {categoriasLocales.length === 0
                ? "No hay categorías disponibles en Solicitudes."
                : "Solo se muestran las categorías ya sincronizadas con Solicitudes."}
              {" "}
              <button
                type="button"
                className="btn btn-link btn-sm p-0 align-baseline"
                onClick={recargarCategoriasLocales}
              >
                Actualizar lista
              </button>
            </div>

            {errorCategoriasLocales && (
              <div className="text-danger mt-1">
                {errorCategoriasLocales}
              </div>
            )}
          </div>

          <div className="mb-3">  {/* Prioridad */}
            <label htmlFor="prioridad" className="form-label">
              Prioridad
            </label>

            {/* form-select: estilo del selector */}
            <select
              id="prioridad"
              className="form-select"
              value={prioridad}
              onChange={(evento) =>
                setPrioridad(evento.target.value)
              }
              disabled={prioridades.length === 0}
            >
              <option value="">
                Selecciona una prioridad
              </option>

              {prioridades.map((prioridadCatalogo) => (
                <option
                  key={prioridadCatalogo.id}
                  value={prioridadCatalogo.nombre}
                >
                  {prioridadCatalogo.nombre}
                </option>
              ))}
            </select>

            <div className="form-text">
              {prioridades.length === 0
                ? "No hay prioridades disponibles."
                : "Selecciona la prioridad de la solicitud."}
            </div>

            {errorPrioridades && (
              <div className="text-danger mt-1">
                {errorPrioridades}
              </div>
            )}
          </div>

          {/* alert: recuadro de aviso; alert-danger: color de error */}
          {errorSolicitud && (
            <div className="alert alert-danger" role="alert">
              {errorSolicitud}
            </div>
          )}

          {/* btn: estilo de botón, btn-primary: color principal (Los colores estan en bootstrap) */}
          <button
            type="button"
            className="btn btn-primary mt-3 me-2"
            onClick={revisarSolicitud}
          >
            Revisar solicitud
          </button>

          {/* btn: estilo de botón; btn-secondary: color secundario */}
          <button
            type="button"
            className="btn btn-secondary mt-3"
            onClick={limpiarFormulario}
          >
            Limpiar formulario
          </button>

          {/* alert: recuadro de aviso, alert-info: color informativo, mt-3: margen superior*/}
          {solicitudRevisada && (
            <div className="alert alert-info mt-3">
              <h3>Resumen de la solicitud</h3>
              <p>Título: {solicitudRevisada.titulo}</p>
              <p>Descripción: {solicitudRevisada.descripcion}</p>
              <p>Categoría: {solicitudRevisada.categoriaNombre}</p>
              <p>Prioridad: {solicitudRevisada.prioridad}</p>
              <p>Vista previa. La solicitud todavía no se ha enviado.</p>
              <button
                type="button"
                className="btn btn-success"
                onClick={enviarSolicitud}
                disabled={enviandoSolicitud}
              >
                {enviandoSolicitud ? "Enviando..." : "Enviar solicitud"}
              </button>
            </div>
          )}
          {solicitudCreada && (
            <div className="alert alert-success mt-3">
              <h3>Solicitud creada correctamente</h3>
              <p>ID: {solicitudCreada.id}</p>
              <p>Título: {solicitudCreada.titulo}</p>
              <p>Categoría: {solicitudCreada.categoriaNombre}</p>
              <p>Estado: {mostrarEstado(solicitudCreada.estado)}</p>
            </div>
          )}

        </div>

        {/*  ----------------- TARJETA PARA VER MIS SOLICITUDES  ----------------- */}
        <div className="card p-3 mb-3">
          <h2>
            {esGestorSolicitudes
              ? "Gestión de solicitudes"
              : "Mis solicitudes"}
          </h2>

          {mensajeEstado && (
            <div className="alert alert-success">
              {mensajeEstado}
            </div>
          )}

          {errorEstado && (
            <div className="alert alert-danger">
              {errorEstado}
            </div>
          )}

          {solicitudes.length === 0 ? (
            <p>No tienes solicitudes registradas.</p>
          ) : (
            <div className="table-responsive">
              <table className="table table-striped">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Título</th>
                    <th>Categoría</th>
                    {esGestorSolicitudes && <th>Usuario</th>}
                    <th>Prioridad</th>
                    <th>Estado</th>
                    <th>Fecha</th>
                  </tr>
                </thead>

                <tbody>
                  {solicitudes.map((solicitud) => (
                    <tr key={solicitud.id}>
                      <td>{solicitud.id}</td>
                      <td>{solicitud.titulo}</td>
                      <td>{solicitud.categoriaNombre || "Sin categoría"}</td>
                      {esGestorSolicitudes && (
                        <td>{solicitud.usuario}</td>
                      )}
                      <td>{solicitud.prioridad}</td>
                      <td>
                        {esGestorSolicitudes ? (
                          <select
                            className="form-select"
                            value={solicitud.estado}
                            onChange={(evento) =>
                              actualizarEstado(
                                solicitud.id,
                                evento.target.value
                              )
                            }
                            disabled={
                              solicitudActualizandoId === solicitud.id ||
                              obtenerEstadosPermitidos(
                                solicitud.estado
                              ).length === 0
                            }
                          >
                            <option value={solicitud.estado}>
                              {mostrarEstado(solicitud.estado)}
                            </option>

                            {obtenerEstadosPermitidos(
                              solicitud.estado
                            ).map((estadoPermitido) => (
                              <option
                                key={estadoPermitido}
                                value={estadoPermitido}
                              >
                                {mostrarEstado(estadoPermitido)}
                              </option>
                            ))}
                          </select>
                        ) : (
                          mostrarEstado(solicitud.estado)
                        )}
                      </td>
                      <td>
                        {solicitud.fechaCreacion?.replace("T", " ")}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
        {/* ----------------- BOTÓN PARA CERRAR LA SESIÓN  ----------------- */}
        <button onClick={cerrarSesion} className="btn btn-danger">
          Cerrar sesión
        </button>
      </AuthenticatedTemplate>

    </div>
  );
}

export default App;
