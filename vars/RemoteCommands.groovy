#!/usr/bin/env groovy

// Виконує команду локально або через SSH, залежно від параметрів
def executeCommand(String ip, String command, String user = 'root', String credentialsId = 'ssh-credentials', boolean useLocalExecution = false) {
    def result = ""
    
    if (useLocalExecution) {
        // Виконуємо команду локально
        result = sh(script: "${command}", returnStdout: true).trim()
    } else {
        // Виконуємо команду через SSH
        withCredentials([sshUserPrivateKey(credentialsId: credentialsId, keyFileVariable: 'SSH_KEY')]) {
            result = sh(script: "ssh -i ${SSH_KEY} -o StrictHostKeyChecking=no ${user}@${ip} '${command}'", returnStdout: true).trim()
        }
    }
    
    return result
}

// Збирає інформацію про встановлені пакети
def collectPackageInfo(String ip, String user = 'root', String credentialsId = 'ssh-credentials', boolean useLocalExecution = false) {
    def result = [:]
    result.osInfo = ""
    result.packageManagers = []
    result.packageInfo = "=== Інформація про встановлені пакети ===\n\n"
    
    try {
        // Отримуємо інформацію про ОС
        if (useLocalExecution) {
            // Для macOS використовуємо sw_vers замість /etc/os-release
            result.osInfo = executeCommand(ip, "sw_vers || cat /etc/os-release", user, credentialsId, useLocalExecution)
        } else {
            result.osInfo = executeCommand(ip, "cat /etc/os-release", user, credentialsId, useLocalExecution)
        }
        
        // Перевіряємо наявність пакетних менеджерів
        def hasBrew = !executeCommand(ip, "which brew || echo 'not found'", user, credentialsId, useLocalExecution).contains("not found")
        def hasDnf = !executeCommand(ip, "which dnf || echo 'not found'", user, credentialsId, useLocalExecution).contains("not found")
        def hasApt = !executeCommand(ip, "which apt || echo 'not found'", user, credentialsId, useLocalExecution).contains("not found")
        def hasYum = !executeCommand(ip, "which yum || echo 'not found'", user, credentialsId, useLocalExecution).contains("not found")
        
        // Для macOS перевіряємо Homebrew
        if (hasBrew) {
            result.packageManagers.add("brew")
            result.packageInfo += "=== Homebrew Packages ===\n"
            result.packageInfo += executeCommand(ip, "brew list --versions", user, credentialsId, useLocalExecution) + "\n\n"
        }
        
        if (hasDnf) {
            result.packageManagers.add("dnf")
            result.packageInfo += "=== DNF Packages ===\n"
            result.packageInfo += executeCommand(ip, "dnf list installed", user, credentialsId, useLocalExecution) + "\n\n"
        }
        
        if (hasApt) {
            result.packageManagers.add("apt")
            result.packageInfo += "=== APT Packages ===\n"
            result.packageInfo += executeCommand(ip, "apt list --installed", user, credentialsId, useLocalExecution) + "\n\n"
        }
        
        if (hasYum && !hasDnf) {
            result.packageManagers.add("yum")
            result.packageInfo += "=== YUM Packages ===\n"
            result.packageInfo += executeCommand(ip, "yum list installed", user, credentialsId, useLocalExecution) + "\n\n"
        }
        
        // Перевіряємо Python пакети
        def hasPip3 = !executeCommand(ip, "which pip3 || echo 'not found'", user, credentialsId, useLocalExecution).contains("not found")
        def hasPip = !executeCommand(ip, "which pip || echo 'not found'", user, credentialsId, useLocalExecution).contains("not found")
        
        result.packageInfo += "=== Python Packages ===\n"
        
        if (hasPip3) {
            result.packageManagers.add("pip3")
            result.packageInfo += "=== Pip3 Packages ===\n"
            result.packageInfo += executeCommand(ip, "pip3 list", user, credentialsId, useLocalExecution) + "\n\n"
        } else {
            result.packageInfo += "pip3 не знайдено на системі\n\n"
        }
        
        if (hasPip && !hasPip3) {
            result.packageManagers.add("pip")
            result.packageInfo += "=== Pip Packages ===\n"
            result.packageInfo += executeCommand(ip, "pip list", user, credentialsId, useLocalExecution) + "\n\n"
        } else if (!hasPip3) {
            result.packageInfo += "pip не знайдено на системі\n\n"
        }
    } catch (Exception e) {
        result.error = "Помилка збору інформації: ${e.message}"
    }
    
    return result
}

// Повертаємо об'єкт
return this