package com.obratech.service;

import com.obratech.entity.*;
import com.obratech.entity.enums.EstadoPostulacion;
import com.obratech.entity.enums.EstadoValidacion;
import com.obratech.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Order(1)
public class SeedService implements CommandLineRunner {

    private final UsuarioRepository usuarioRepo;
    private final PerfilRepository perfilRepo;
    private final ProyectoRepository proyectoRepo;
    private final PostulacionRepository postulacionRepo;
    private final CalificacionRepository calificacionRepo;
    private final EquipoTrabajoRepository equipoTrabajoRepo;
    private final PasswordEncoder passwordEncoder;
    private final MongoTemplate mongoTemplate;

    private final Random rnd = new Random();

    @Value("${app.seed.enabled:false}")
    private boolean enabled;

    public SeedService(
            UsuarioRepository usuarioRepo,
            PerfilRepository perfilRepo,
            ProyectoRepository proyectoRepo,
            PostulacionRepository postulacionRepo,
            CalificacionRepository calificacionRepo,
            EquipoTrabajoRepository equipoTrabajoRepo,
            PasswordEncoder passwordEncoder,
            MongoTemplate mongoTemplate) {

        this.usuarioRepo = usuarioRepo;
        this.perfilRepo = perfilRepo;
        this.proyectoRepo = proyectoRepo;
        this.postulacionRepo = postulacionRepo;
        this.calificacionRepo = calificacionRepo;
        this.equipoTrabajoRepo = equipoTrabajoRepo;
        this.passwordEncoder = passwordEncoder;
        this.mongoTemplate = mongoTemplate;
    }

    public void initSystem() {
        crearProyectos();
    }

    @Override
    public void run(String... args) {

        if (!enabled) {
            System.out.println("[Seed] desactivado");
            return;
        }

        crearAdmin();
        crearUsuariosYPerfiles();
        crearTrabajadores();
        crearProyectos();
        crearPostulaciones();
        crearCalificaciones();

        System.out.println("[Seed] ejecucin finalizada correctamente");
    }

    // ================= ADMIN =================
    private void crearAdmin() {
        if (usuarioRepo.findByUsername("admin@gmail.com").isEmpty()) {
            Usuario admin = new Usuario();
            admin.setUsername("admin@gmail.com");
            admin.setPassword(passwordEncoder.encode("administrador2026"));
            admin.setRole("ROLE_ADMIN");
            usuarioRepo.save(admin);
            System.out.println("[Seed] Admin creado");
        }
    }

    // ================= USUARIOS + PERFILES EN personas =================
    private void crearUsuariosYPerfiles() {

        List<String> roles = List.of("ROLE_CLIENT", "ROLE_CONTRACTOR");

        for (int i = 0; i < 100; i++) {
            String email = "user" + String.format("%04d", i) + "@example.com";
            String role = roles.get(i % 2); // alternar cliente / contratista

            // Crear usuario si no existe
            if (usuarioRepo.findByUsername(email).isEmpty()) {
                Usuario u = new Usuario();
                u.setUsername(email);
                u.setPassword(passwordEncoder.encode("1234"));
                u.setRole(role);
                usuarioRepo.save(u);
            }

            // Crear perfil en `perfiles` si no existe
            if (perfilRepo.findByUsername(email).isEmpty()) {
                Perfil p = new Perfil();
                p.setUsername(email);
                p.setEmail(email);
                p.setRole(role);
                p.setActivo(true);
                p.setVerificado(false);

                if ("ROLE_CLIENT".equals(role)) {
                    p.setNombre("Cliente " + i);
                    p.setEmpresa("Empresa " + i);
                } else {
                    p.setNombre("Contratista " + i);
                    p.setEspecialidad("Construccin");
                    p.setCalificacionPromedio(0.0);
                    p.setExperiencia(rnd.nextInt(10) + 1);
                }

                perfilRepo.save(p);
            }
        }

        System.out.println("[Seed] Usuarios y perfiles listos");
    }

    // ================= TRABAJADORES =================
    private void crearTrabajadores() {

        long existentes = perfilRepo.findByRoles("ROLE_WORKER").size();
        if (existentes > 0) return;

        String[] oficios = {"Albail", "Electricista", "Fontanero", "Pintor", "Carpintero"};

        for (int i = 0; i < 30; i++) {
            String email = "trabajador" + i + "@example.com";

            // Crear usuario para el trabajador
            if (usuarioRepo.findByUsername(email).isEmpty()) {
                Usuario u = new Usuario();
                u.setUsername(email);
                u.setPassword(passwordEncoder.encode("1234"));
                u.setRole("ROLE_WORKER");
                usuarioRepo.save(u);
            }

            // Crear perfil en perfiles
            if (perfilRepo.findByUsername(email).isEmpty()) {
                Perfil p = new Perfil();
                p.setUsername(email);
                p.setEmail(email);
                p.setRole("ROLE_WORKER");
                p.setNombre("Trabajador " + i);
                p.setApellido("Demo");
                p.setOficio(oficios[i % oficios.length]);
                p.setExperiencia(rnd.nextInt(10));
                p.setDisponibilidad(true);
                p.setActivo(true);
                p.setVerificado(false);
                perfilRepo.save(p);
            }
        }

        System.out.println("[Seed] Trabajadores listos");
    }

    // ================= PROYECTOS =================
    private void crearProyectos() {

        if (proyectoRepo.count() > 0) return;

        for (int i = 0; i < 50; i++) {
            Proyecto p = new Proyecto();
            p.setTitulo("Proyecto " + i);
            p.setDescripcion("Demo proyecto");
            p.setPresupuesto(10000.0 + rnd.nextInt(50000));
            p.setEstadoValidacion(EstadoValidacion.PENDIENTE);

            proyectoRepo.save(p);
        }
    }

    // ================= POSTULACIONES =================
    private void crearPostulaciones() {

        if (postulacionRepo.count() > 0) return;

        List<Usuario> usuarios = usuarioRepo.findAll();
        List<Proyecto> proyectos = proyectoRepo.findAll();

        for (int i = 0; i < 50 && !usuarios.isEmpty() && !proyectos.isEmpty(); i++) {

            Postulacion p = new Postulacion();
            p.setUsuario(usuarios.get(rnd.nextInt(usuarios.size())));
            p.setProyecto(proyectos.get(rnd.nextInt(proyectos.size())));
            p.setEstado(EstadoPostulacion.PENDING);

            postulacionRepo.save(p);
        }
    }

    // ================= CALIFICACIONES =================
    private void crearCalificaciones() {

        if (calificacionRepo.count() > 0) return;

        List<Proyecto> proyectos = proyectoRepo.findAll();

        for (int i = 0; i < 20 && !proyectos.isEmpty(); i++) {
            Calificacion c = new Calificacion();
            c.setComentario("Buen trabajo");
            c.setPuntuacion(3 + rnd.nextInt(3));
            c.setProyecto(proyectos.get(rnd.nextInt(proyectos.size())));
            calificacionRepo.save(c);
        }
    }

    // ================= MASSIVE SEEDING OF 15,000 DOCUMENTS =================
    public void seed15kData() {
        System.out.println("[Seed15k] Iniciando generación masiva de 15,000 registros...");
        
        // 1. Limpiar colecciones anteriores para garantizar que no duplicamos/mezclamos
        usuarioRepo.deleteAll();
        perfilRepo.deleteAll();
        proyectoRepo.deleteAll();
        postulacionRepo.deleteAll();
        calificacionRepo.deleteAll();
        equipoTrabajoRepo.deleteAll();
        
        // Crear Admin obligatorio para acceso al panel
        Usuario admin = new Usuario();
        admin.setUsername("admin@gmail.com");
        admin.setPassword(passwordEncoder.encode("administrador2026"));
        admin.setRole("ROLE_ADMIN");
        usuarioRepo.save(admin);
        
        List<Usuario> usuarios = new ArrayList<>();
        List<Perfil> perfiles = new ArrayList<>();
        
        String[] oficios = {"Albañil", "Electricista", "Fontanero", "Pintor", "Carpintero", "Soldador", "Yesero", "Maestro de Obra"};
        String[] especialidades = {"Construcción Civil", "Instalaciones Eléctricas", "Acabados y Pintura", "Estructuras Metálicas", "Carpintería y Muebles", "Diseño de Interiores", "Cimentaciones"};
        String[] empresas = {"Constructora Alfa S.A.", "Obras y Diseños Beta", "Proyectos Gamma S.A.C.", "Edificaciones Delta", "Inmobiliaria Sigma", "Desarrollos Omega"};
        String[] nombresHombres = {"Juan", "Pedro", "Carlos", "Luis", "Miguel", "Jorge", "Andrés", "Javier", "David", "José", "Francisco", "Manuel", "Santiago", "Sebastián", "Alejandro"};
        String[] nombresMujeres = {"María", "Ana", "Laura", "Sofia", "Andrea", "Camila", "Elena", "Isabella", "Valentina", "Gabriela", "Mariana", "Lucía", "Beatriz", "Clara"};
        String[] apellidos = {"Gómez", "Rodríguez", "López", "Martínez", "González", "Pérez", "Sánchez", "Ramírez", "Torres", "Flores", "Díaz", "Vásquez", "Ruiz", "Morales", "Castro", "Ortiz"};
        
        String passwordEncriptado = passwordEncoder.encode("1234");
        
        // --- Generar 5,000 Usuarios + 5,000 Perfiles (10,000 documentos) ---
        // 2,000 Workers, 1,500 Clients, 1,500 Contractors
        for (int i = 1; i <= 5000; i++) {
            String role;
            if (i <= 2000) {
                role = "ROLE_WORKER";
            } else if (i <= 3500) {
                role = "ROLE_CLIENT";
            } else {
                role = "ROLE_CONTRACTOR";
            }
            
            String email = "usuario" + i + "@example.com";
            
            // Usuario
            Usuario u = new Usuario();
            u.setUsername(email);
            u.setPassword(passwordEncriptado);
            u.setRole(role);
            u.setActivo(true);
            u.setVerificado(true);
            usuarios.add(u);
            
            // Perfil
            Perfil p = new Perfil();
            p.setUsername(email);
            p.setEmail(email);
            p.setRole(role);
            p.setActivo(true);
            p.setVerificado(true);
            
            String nombre = (rnd.nextBoolean() ? nombresHombres[rnd.nextInt(nombresHombres.length)] : nombresMujeres[rnd.nextInt(nombresMujeres.length)]);
            String apellido = apellidos[rnd.nextInt(apellidos.length)] + " " + apellidos[rnd.nextInt(apellidos.length)];
            p.setNombre(nombre);
            p.setApellido(apellido);
            p.setTelefono("+51 9" + (10000000 + rnd.nextInt(90000000)));
            
            if ("ROLE_WORKER".equals(role)) {
                p.setOficio(oficios[rnd.nextInt(oficios.length)]);
                p.setDisponibilidad(rnd.nextBoolean());
            } else if ("ROLE_CLIENT".equals(role)) {
                p.setEmpresa(empresas[rnd.nextInt(empresas.length)]);
            } else if ("ROLE_CONTRACTOR".equals(role)) {
                p.setEspecialidad(especialidades[rnd.nextInt(especialidades.length)]);
                p.setUbicacion("Lima, Perú");
                p.setExperiencia(rnd.nextInt(20) + 1);
                p.setCalificacionPromedio(3.0 + rnd.nextDouble() * 2.0);
                p.setDescripcion("Contratista con amplia experiencia en proyectos de ingeniería y edificaciones.");
            }
            perfiles.add(p);
        }
        
        System.out.println("[Seed15k] Guardando 5000 usuarios...");
        mongoTemplate.insertAll(usuarios);
        System.out.println("[Seed15k] Guardando 5000 perfiles...");
        mongoTemplate.insertAll(perfiles);
        
        // Clasificar para referencias
        List<Perfil> trabajadores = new ArrayList<>();
        List<Perfil> contratistas = new ArrayList<>();
        List<Usuario> clientes = new ArrayList<>();
        
        for (Perfil p : perfiles) {
            if ("ROLE_WORKER".equals(p.getRole())) {
                trabajadores.add(p);
            } else if ("ROLE_CONTRACTOR".equals(p.getRole())) {
                contratistas.add(p);
            }
        }
        for (Usuario u : usuarios) {
            if ("ROLE_CLIENT".equals(u.getRole())) {
                clientes.add(u);
            }
        }
        
        // --- Generar 2,000 Proyectos (2,000 documentos) ---
        List<Proyecto> proyectos = new ArrayList<>();
        String[] tiposProyecto = {"Residencial", "Comercial", "Industrial", "Infraestructura", "Remodelación"};
        String[] ubicaciones = {"Lima", "Arequipa", "Trujillo", "Chiclayo", "Piura", "Cusco", "Huancayo"};
        
        for (int i = 1; i <= 2000; i++) {
            Proyecto pr = new Proyecto();
            pr.setTitulo("Proyecto Edificación " + i);
            pr.setDescripcion("Construcción e ingeniería de obra de tipo " + tiposProyecto[i % tiposProyecto.length] + ". Contempla acabados, cimientos y estructuras.");
            pr.setTipoProyecto(tiposProyecto[rnd.nextInt(tiposProyecto.length)]);
            pr.setUbicacion(ubicaciones[rnd.nextInt(ubicaciones.length)]);
            pr.setPresupuesto(15000.0 + rnd.nextInt(500000));
            pr.setPlazoEstimado(30 + rnd.nextInt(360));
            pr.setFechaInicio(java.time.LocalDate.now().plusDays(rnd.nextInt(60)));
            pr.setFechaEntrega(pr.getFechaInicio().plusDays(pr.getPlazoEstimado()));
            pr.setEstadoValidacion(EstadoValidacion.APROBADO);
            pr.setEstadoEjecucion(com.obratech.entity.enums.EstadoEjecucion.values()[rnd.nextInt(com.obratech.entity.enums.EstadoEjecucion.values().length)]);
            pr.setEstadoAsignacion(com.obratech.entity.enums.EstadoAsignacion.ASIGNADO);
            
            if (!clientes.isEmpty()) {
                pr.setCliente(clientes.get(rnd.nextInt(clientes.size())));
            }
            if (!contratistas.isEmpty()) {
                pr.setContratistaAsignado(contratistas.get(rnd.nextInt(contratistas.size())));
            }
            
            proyectos.add(pr);
        }
        System.out.println("[Seed15k] Guardando 2000 proyectos...");
        mongoTemplate.insertAll(proyectos);
        
        // --- Generar 1,500 Postulaciones (1,500 documentos) ---
        List<Postulacion> postulaciones = new ArrayList<>();
        for (int i = 1; i <= 1500; i++) {
            Postulacion pos = new Postulacion();
            pos.setUsuario(usuarios.get(rnd.nextInt(usuarios.size())));
            pos.setProyecto(proyectos.get(rnd.nextInt(proyectos.size())));
            pos.setEstado(EstadoPostulacion.values()[rnd.nextInt(EstadoPostulacion.values().length)]);
            pos.setMensaje("Propuesta para la obra " + i);
            pos.setFechaPostulacion(java.time.LocalDateTime.now().minusDays(rnd.nextInt(30)));
            postulaciones.add(pos);
        }
        System.out.println("[Seed15k] Guardando 1500 postulaciones...");
        mongoTemplate.insertAll(postulaciones);
        
        // --- Generar 1,000 Calificaciones (1,000 documentos) ---
        List<Calificacion> calificaciones = new ArrayList<>();
        String[] comentarios = {"Excelente servicio y puntualidad", "Muy recomendado para acabados", "Buen contratista, aunque hubo un ligero retraso", "Trabajo limpio y ordenado", "Cumplió con todas las especificaciones"};
        
        for (int i = 1; i <= 1000; i++) {
            Calificacion cal = new Calificacion();
            cal.setPuntuacion(3 + rnd.nextInt(3));
            cal.setComentario(comentarios[rnd.nextInt(comentarios.length)]);
            cal.setProyecto(proyectos.get(rnd.nextInt(proyectos.size())));
            if (!contratistas.isEmpty()) {
                cal.setContratista(contratistas.get(rnd.nextInt(contratistas.size())));
            }
            if (!clientes.isEmpty()) {
                cal.setCliente(clientes.get(rnd.nextInt(clientes.size())));
            }
            calificaciones.add(cal);
        }
        System.out.println("[Seed15k] Guardando 1000 calificaciones...");
        mongoTemplate.insertAll(calificaciones);
        
        // --- Generar 500 Equipos de Trabajo (500 documentos) ---
        List<EquipoTrabajo> equipos = new ArrayList<>();
        String[] nombresEquipos = {"Cuadrilla Albañilería", "Cuadrilla Electricistas", "Equipo Cimentación", "Equipo Acabados", "Cuadrilla Estructuras", "Equipo Fontanería"};
        String[] actividadesEquipos = {"Levantamiento de muros", "Cableado general e iluminación", "Vaciado de columnas y vigas", "Pintura y colocación de cerámicos", "Instalaciones sanitarias"};
        
        for (int i = 1; i <= 500; i++) {
            EquipoTrabajo eq = new EquipoTrabajo();
            eq.setNombre(nombresEquipos[rnd.nextInt(nombresEquipos.length)] + " " + i);
            eq.setActividad(actividadesEquipos[rnd.nextInt(actividadesEquipos.length)]);
            eq.setPorcentajeAvance(rnd.nextInt(101) * 1.0);
            eq.setProyecto(proyectos.get(rnd.nextInt(proyectos.size())));
            
            int numIntegrantes = 2 + rnd.nextInt(4);
            List<Perfil> ints = new ArrayList<>();
            for (int j = 0; j < numIntegrantes && !trabajadores.isEmpty(); j++) {
                ints.add(trabajadores.get(rnd.nextInt(trabajadores.size())));
            }
            eq.setIntegrantes(ints);
            equipos.add(eq);
        }
        System.out.println("[Seed15k] Guardando 500 equipos de trabajo...");
        mongoTemplate.insertAll(equipos);
        
        System.out.println("[Seed15k] Generación masiva completada con éxito. Total: 15,000 registros insertados.");
    }
}
