package testingforest

class ProjectController {

    def projectService

    def addUserProject(){ // добавление к проекту юзера
    }

    def addingUser(){
        def currUser = User.findByLogin(params.login)
        if(currUser){ //если он существует и если у него нет проекта
            def currProject = Project.get(session.project.id)
            def teamList = project.getTeamList()
            def sessionUser = session.user
            def result = teamList.find{member -> if (member != null) member.login.equals(sessionUser.login)}
            if(result == null) {
                currProject.addToTeamList(currUser).save(flush: true)
                flash.message = "User $currUser has already been in project!"
                redirect(action: "addUserProject")
            }
        }
        else{
            flash.message = "Sorry. Please try another login."
            redirect(action: "addUserProject")
        }
    }

    def backToShow(){
        redirect(action: "show", id:  session.project.id)
    }

    def index() {
        session.project = null //обнуляем текущий проект
        def projectList = []
        for(Project project:Project.all) {
            def teamList = project.getTeamList()
            def sessionUser = session.user
            def result = teamList.find{member -> if (member != null) member.login.equals(sessionUser.login)}
            if(result)
                projectList.add(project)
        }
        respond projectList
    }

    def show(Long id) {
        session.project = Project.get(id) //текущий проект в show
        respond projectService.get(id)
    }

    def create() {
        respond new Project(params)

    }

    def save(Project project) {
        project.addToTeamList(session.user).save(flush: true)
        redirect action:"index"
    }

}