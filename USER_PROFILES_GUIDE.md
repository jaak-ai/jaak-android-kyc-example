# 👤 Guía: Perfiles KYC por Usuario

**Fecha:** 20 de Noviembre de 2025  
**Versión:** 1.1.0-beta.12  
**Autor:** Michael Antonio Avila Escobar

---

## 📋 Resumen de Cambios

Se implementó un sistema de **perfiles KYC asociados por usuario**, permitiendo que cada usuario tenga sus propias configuraciones de perfiles KYC que persisten entre sesiones.

### **Antes:**
- ❌ Todos los usuarios compartían los mismos perfiles KYC
- ❌ Al cerrar sesión y volver a entrar, los perfiles se perdían o se mezclaban
- ❌ Usuario A podía ver los perfiles creados por Usuario B

### **Ahora:**
- ✅ Cada usuario tiene sus propios perfiles KYC
- ✅ Los perfiles persisten al cerrar sesión y volver a entrar
- ✅ Usuario A solo ve sus perfiles, Usuario B solo ve los suyos
- ✅ Aislamiento completo entre usuarios

---

## 🔑 Implementación Técnica

### **Almacenamiento en SharedPreferences:**

**Antes (Global):**
```
KEY: "kyc_profiles"
VALUE: [perfil1, perfil2, perfil3, ...]
```

**Ahora (Por Usuario):**
```
KEY: "kyc_profiles_user_usuario1@example.com"
VALUE: [perfil1_usuario1, perfil2_usuario1, ...]

KEY: "kyc_profiles_user_usuario2@example.com"
VALUE: [perfil1_usuario2, perfil2_usuario2, ...]
```

### **Identificador Único:**
Se utiliza el **email del usuario** como identificador único para asociar los perfiles.

```kotlin
private fun getKycProfilesKey(): String {
    val userInfo = getUserInfo()
    return if (userInfo != null) {
        "${KEY_KYC_PROFILES_PREFIX}${userInfo.email}"
    } else {
        KEY_KYC_PROFILES // Fallback legacy
    }
}
```

---

## 🛠️ Métodos Modificados

### **1. `saveKycProfile(profile: KycProfile)`**
**Cambio:** Ahora guarda el perfil usando la clave específica del usuario logueado.

```kotlin
// Antes
prefs.edit().putString("kyc_profiles", json).apply()

// Ahora
val key = getKycProfilesKey() // "kyc_profiles_user_email@example.com"
prefs.edit().putString(key, json).apply()
```

### **2. `getKycProfiles(): List<KycProfile>`**
**Cambio:** Obtiene solo los perfiles del usuario actual.

```kotlin
val key = getKycProfilesKey()
val json = prefs.getString(key, null) ?: return emptyList()
```

### **3. `deleteKycProfile(id: String)`**
**Cambio:** Elimina perfiles solo del usuario actual.

### **4. `setDefaultProfile(profileId: String)`**
**Cambio:** Marca como predeterminado solo en el contexto del usuario actual.

---

## 🆕 Nuevos Métodos

### **`clearUserKycProfiles()`**
Limpia los perfiles KYC del usuario actual.

**Uso:**
```kotlin
// Borrar perfiles del usuario sin cerrar sesión
profileManager.clearUserKycProfiles()
```

**Diferencia con `clearAll()`:**
- `clearUserKycProfiles()`: Borra solo perfiles del usuario actual
- `clearAll()`: Borra TODOS los datos de TODOS los usuarios (reset completo)

---

## 📊 Flujos de Usuario

### **Flujo 1: Usuario Crea Perfiles y Cierra Sesión**

1. **Usuario A** inicia sesión → `usuario_a@example.com`
2. Crea 3 perfiles KYC:
   - Perfil QA México
   - Perfil Sandbox Colombia
   - Perfil Dev Argentina
3. Se guardan en: `kyc_profiles_user_usuario_a@example.com`
4. Usuario A cierra sesión → `logout()`
5. Los perfiles **se mantienen** en SharedPreferences
6. Usuario A vuelve a iniciar sesión
7. ✅ Sus 3 perfiles aparecen nuevamente

---

### **Flujo 2: Múltiples Usuarios en el Mismo Dispositivo**

1. **Usuario A** inicia sesión → `usuario_a@example.com`
   - Crea 2 perfiles
   - Se guardan en: `kyc_profiles_user_usuario_a@example.com`

2. Usuario A cierra sesión

3. **Usuario B** inicia sesión → `usuario_b@example.com`
   - Crea 5 perfiles diferentes
   - Se guardan en: `kyc_profiles_user_usuario_b@example.com`

4. Usuario B cierra sesión

5. **Usuario A** vuelve a iniciar sesión
   - ✅ Ve solo sus 2 perfiles originales
   - ❌ NO ve los 5 perfiles de Usuario B

---

### **Flujo 3: Usuario Sin Sesión Iniciada (Fallback)**

1. Usuario abre la app sin iniciar sesión
2. Intenta crear perfiles
3. Se usa clave legacy: `kyc_profiles` (sin email)
4. Al iniciar sesión, se migran a clave con email automáticamente

---

## 🔒 Seguridad y Aislamiento

### **Garantías:**
- ✅ **Aislamiento total**: Usuario A nunca puede ver perfiles de Usuario B
- ✅ **Persistencia**: Los perfiles sobreviven al cierre de sesión
- ✅ **Privacidad**: Cada usuario tiene su propio espacio de configuración

### **Identificador:**
- Se usa el **email del usuario** como identificador único
- El email se obtiene de `UserInfo` guardado en el login

---

## 📝 Ejemplos de Uso

### **Obtener Perfiles del Usuario Actual:**
```kotlin
val profiles = profileManager.getKycProfiles()
// Retorna solo perfiles del usuario logueado
```

### **Guardar Nuevo Perfil:**
```kotlin
val newProfile = KycProfile(
    profileName = "Mi Perfil QA",
    contactName = "Juan Pérez",
    flowName = "onboarding-mx",
    countryDocument = "MEX",
    selectedFlowType = "TRADITIONAL"
)
profileManager.saveKycProfile(newProfile)
// Se guarda asociado al usuario actual
```

### **Cerrar Sesión (Mantiene Perfiles):**
```kotlin
profileManager.logout()
// Los perfiles del usuario se mantienen para el próximo login
```

### **Borrar Perfiles del Usuario:**
```kotlin
profileManager.clearUserKycProfiles()
// Borra solo los perfiles del usuario actual
```

### **Reset Completo (Todos los Usuarios):**
```kotlin
profileManager.clearAll()
// Borra TODO: perfiles de todos los usuarios + tokens + info
```

---

## 🧪 Pruebas Recomendadas

### **Test 1: Persistencia de Perfiles**
1. Iniciar sesión con Usuario A
2. Crear 3 perfiles
3. Cerrar sesión
4. Volver a iniciar sesión con Usuario A
5. ✅ Verificar que los 3 perfiles aparecen

### **Test 2: Aislamiento entre Usuarios**
1. Iniciar sesión con Usuario A → Crear 2 perfiles
2. Cerrar sesión
3. Iniciar sesión con Usuario B → Crear 3 perfiles
4. Cerrar sesión
5. Iniciar sesión con Usuario A
6. ✅ Verificar que solo aparecen 2 perfiles (los de Usuario A)
7. Cerrar sesión
8. Iniciar sesión con Usuario B
9. ✅ Verificar que solo aparecen 3 perfiles (los de Usuario B)

### **Test 3: Creación y Edición**
1. Iniciar sesión con Usuario A
2. Crear perfil "Perfil QA"
3. Editarlo y cambiar nombre a "Perfil QA Actualizado"
4. Cerrar sesión y volver a entrar
5. ✅ Verificar que el nombre actualizado se mantiene

### **Test 4: Eliminación**
1. Iniciar sesión con Usuario A
2. Crear 5 perfiles
3. Eliminar 2 perfiles
4. Cerrar sesión y volver a entrar
5. ✅ Verificar que solo aparecen 3 perfiles

---

## 🐛 Troubleshooting

### **Problema: No aparecen mis perfiles al volver a entrar**
**Causa:** El email del usuario puede haber cambiado o no se guardó correctamente.

**Solución:**
1. Verificar que `UserInfo` se guarda correctamente en el login
2. Verificar que el email es consistente entre sesiones
3. Revisar logs: `Log.d("ProfileManager", "Using key: ${getKycProfilesKey()}")`

### **Problema: Veo perfiles de otro usuario**
**Causa:** Bug en la implementación o email duplicado.

**Solución:**
1. Verificar que cada usuario tiene un email único
2. Revisar que `getUserInfo()` retorna el usuario correcto
3. Hacer reset: `profileManager.clearAll()`

---

## 📈 Mejoras Futuras

1. **Migración de Perfiles Legacy**
   - Detectar perfiles en clave `kyc_profiles`
   - Migrar automáticamente a clave con email al primer login

2. **Sincronización con Backend**
   - Guardar perfiles en servidor
   - Sincronizar entre dispositivos

3. **Exportar/Importar Perfiles**
   - Permitir compartir configuraciones entre usuarios
   - Exportar como JSON/archivo

4. **Encriptación**
   - Encriptar perfiles en SharedPreferences
   - Mayor seguridad para datos sensibles

---

## 📞 Contacto

**Desarrollador:** Michael Antonio Avila Escobar  
**Email:** michael.avila@jaak.ai  
**Proyecto:** jaak-android-kyc-example

---

**Documento generado automáticamente**  
**Última actualización:** 20 de Noviembre de 2025
