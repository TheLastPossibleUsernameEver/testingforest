package testingforest

import grails.validation.ValidationException
import org.apache.commons.lang.RandomStringUtils

import static org.springframework.http.HttpStatus.*

class UserController {

    UserService userService

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def log_in() {}

    def authenticate() {
        def hexPassword = params.password.encodeAsSHA1()
        def user = User.findByLoginAndPassword(params.login, hexPassword)
        if(user){
            session.user = user

            log.info("User ${session.user.login} logged in")

            flash.message = message(code:"login.message", args: [session.user.name])
            redirect uri: "/project/index"
        }
        else{
            flash.error = message(code:"login.error")
            log.error("User's authentication failed")
            redirect uri: "/user/log_in"
        }
    }

    def logout() {
        flash.message = message(code:"logout.message", args: [session.user.name])

        log.info("User ${session.user.login} logged out")

        session.user = null
        redirect uri: "/user/log_in"
    }
    def showInfo() {
        def user = User.get(session.user.id)
        def testCases = user.caseList
        def criteria = Project.createCriteria()
        def projects = criteria.list{
            teamList{
                idEq(user.id)
            }
        }
        return [projects:projects,testCases:testCases]
    }
    def deleteCurrentUser() {
        def user = User.get(session.user.id)

        if(user.getCaseList().size() > 0){
            for(TestCase testCase : user.getCaseList())
            {
                if(testCase.getTypeCase().equals("public")) {
                    testCase.setUserCreated(null)
                    testCase.save()
                }
                if(testCase.getTypeCase().equals("private"))
                    testCase.delete()
            }
        }

        def criteria = Project.createCriteria()
        def projects = criteria.list{
            teamList{
                idEq(user.id)
            }
        }
        projects.each{
            if ( it.teamList.size() == 1 ){
                it.delete(flush:true)
            } else {
                it.teamList.removeElement(user)
            }
        }
        log.info("User ${session.user.login} was removed")
        User.get(user.id).delete(flush:true)
        session.invalidate()
        redirect uri: "/user/log_in"
    }

    def show(Long id) {
        respond userService.get(id)
    }

    def create() {
        respond new User(params)
    }

    def save(User user) {
        user.role = "user"
        if (user.validate()) {
            /*
            // It does not work due to the spam tracker on the mailing address. FIX!
            sendMail {
                from "testingforest@yandex.ru"
                subject "E-mail confirmation"
                to params.email
                text "Congratulations! You're successfully registered on TestingForest" +
                        ".\nYour login is " + user.getLogin() +
                        ".\nYour current e-mail will be used for password restore if you forgot it."
            }
            */
            user.save()
            log.info("User ${user.login} registered")
            flash.message = message(code: 'registration.success.message', args: [user.name])
            redirect uri: "/user/log_in"
        } else {
            respond user.errors, view: 'create'
            log.error("Incorrect data for user registration")
        }
    }

    def edit(Long userId) {
        respond userService.get(userId)
    }

    def update(User user) {
        if (!user.password) {
            user.password = user.getPersistentValue("password")
        }
        if (user.validate()) {
            user.save(flush: true)
            flash.message = message(code: "user.edit.success.message")
            session.user = user

            log.info("Updated ${user.login} user.")

            redirect uri: "/user/showInfo"
        } else {
            respond user.errors, view: "edit"
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'user.label', default: 'User'), params.id])
                redirect uri: "/user/index"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
}
