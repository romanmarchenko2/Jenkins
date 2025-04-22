pipeline {
    agent any
    
    parameters {
        string(name: 'REMOTE_IP', defaultValue: 'localhost', description: 'IP-адреса віддаленого сервера')
        string(name: 'SSH_USER', defaultValue: 'root', description: 'Користувач SSH')
        string(name: 'SSH_CREDENTIALS_ID', defaultValue: 'ssh-credentials', description: 'ID SSH ключа в Jenkins')
        booleanParam(name: 'USE_LOCAL_EXECUTION', defaultValue: true, description: 'Використовувати локальне виконання команд (без SSH)')
    }
    
    stages {
        stage('Збір інформації про пакети') {
            steps {
                script {
                    // Створюємо директорію для артефактів
                    sh 'mkdir -p package_info'
                    
                    // Завантажуємо функції з файлу
                    def remoteCommands 
                    try {
                        remoteCommands = load "vars/RemoteCommands.groovy"
                        echo "RemoteCommands loaded: ${remoteCommands != null}"
                    } catch (Exception e) {
                        error "Не вдалося завантажити RemoteCommands.groovy: ${e.message}"
                    }
                    
                    // Використовуємо функцію для збору інформації
                    def packageData
                    try {
                        // Викликаємо метод як метод об'єкта remoteCommands
                        packageData = remoteCommands.collectPackageInfo(
                            params.REMOTE_IP, 
                            params.SSH_USER, 
                            params.SSH_CREDENTIALS_ID, 
                            params.USE_LOCAL_EXECUTION
                        )
                        
                        // Зберігаємо інформацію про ОС
                        writeFile file: "package_info/os_info.txt", text: packageData.osInfo
                        
                        // Зберігаємо інформацію про пакети
                        writeFile file: "package_info/installed_packages.txt", text: packageData.packageInfo
                        
                        // Якщо є помилки, зберігаємо їх також
                        if (packageData.error) {
                            writeFile file: "package_info/errors.txt", text: packageData.error
                            echo "Виникли помилки під час збору інформації: ${packageData.error}"
                        }
                        
                        // Виводимо знайдені пакетні менеджери
                        echo "Знайдені пакетні менеджери: ${packageData.packageManagers.join(', ')}"
                        
                        // Виводимо вміст файлу для діагностики
                        sh 'cat package_info/installed_packages.txt'
                    } catch (Exception e) {
                        echo "Помилка виконання скрипту: ${e.message}"
                        writeFile file: "package_info/errors.txt", text: "Помилка виконання скрипту: ${e.message}"
                    }
                }
            }
        }
    }
    
    post {
        always {
            // Зберігаємо файли як артефакти
            archiveArtifacts artifacts: 'package_info/**', fingerprint: true
        }
    }
}