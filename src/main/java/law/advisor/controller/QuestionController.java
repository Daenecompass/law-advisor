package law.advisor.controller;

import law.advisor.model.*;
import law.advisor.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.security.Principal;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Controller
public class QuestionController {

    @Autowired
    EntityManager entityManager;

    @Autowired
    QuestionRepository questionRepository;

    @Autowired
    ContentRepository contentRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    GradeRepository gradeRepository;

    /* This function returns the page with list of questions */
    @RequestMapping(value = {"/question","/question/list"})
    public String list(ModelMap model){

        List<Question> questions=questionRepository.findAll();

        model.addAttribute("questions",questions);


        return "question/list";

    }
    
    /* This function returns the page with the list of questions of a particular category   */
    @RequestMapping(value = {"/question/{category_id}/list"})
    public String getByCategory(@PathVariable("category_id") Long category_id,ModelMap model){

        List<Question> questions=questionRepository.findByCategory_Id(category_id);

        model.addAttribute("questions",questions);
        model.addAttribute("categoryId",category_id);

        return "question/list";
    }

    @RequestMapping("/question/{id}/view")
    public String viewQuestion(ModelMap model,@PathVariable("id") Long id){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user;
        String role;
        try {

            user=userRepository.findUserByUsername(auth.getName());
            role = user.getUserType().toString();
        }
        catch (Exception e){
            role="none";
        }

        Question question1=questionRepository.getOne(id);
        question1.setViews(question1.getViews()+1);
        questionRepository.save(question1);
        String baseQuery="select q.id,u.username as user,q.title,q.date,c.name as category,con.text as content,\n" +
                "       (select count(1) from answer where question_id=q.id) as answers,               (select count(1) from grade where question_id=q.id and type=1) as likes,\n" +
                "       (select count(1) from grade where question_id=q.id and type=2) as disLikes\n" +
                "from question q,category c,content con,user u where q.user_id=u.id and  q.category_id=c.id and q.content_id=con.id and q.id="+id;
        Query query=entityManager.createNativeQuery(baseQuery,QuestionModel.class);
        QuestionModel question= (QuestionModel) query.getSingleResult();



        model.addAttribute("role", role);
        model.addAttribute("question",question);
        return "question/view";
    }


    @RequestMapping("/question/{id}/save")
    public String addQuestion(ModelMap model,@PathVariable("id") long id){

        if(id==0){

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user=userRepository.findUserByUsername(auth.getName());

            Question question=new Question();
            Content content=new Content();
            contentRepository.save(content);
            question.setContent(content);
            question.setDate(new Date());
            question.setUser(user);
            model.addAttribute("question",question);
        }
        else{
            model.addAttribute("question",questionRepository.getOne(id));
        }
        model.addAttribute("categories",categoryRepository.findAll());
        return "question/form";
    }

    @PostMapping("/question/save")
    public String saveQuestion(Question question, String text){

        if(question.getId()==null){
            Content content1=question.getContent();
            content1.setText(text);
            contentRepository.save(content1);
            questionRepository.save(question);
        }
        else if(question.getId()>0){
            Content content1=question.getContent();
            content1.setText(text);
            contentRepository.save(content1);
            questionRepository.save(question);
        }
        return "redirect:question/"+question.getId()+"/view";
    }

    @PostMapping("/question/{id}/delete")
    public String delete(@PathVariable("id") Long id){

        Question question=questionRepository.getOne(id);
        questionRepository.delete(question);

        return "redirect:/";
    }

    @GetMapping("/question/{searchStr}/search")
    public String search(@PathVariable(value = "searchStr",required = false) String searchStr,ModelMap model){

        if(searchStr.equals(" ")){
            searchStr="";
        }
        String baseQuery="select q.id,u.username as user,q.title,q.date,c.name as category,con.text as content,\n" +
                "       (select count(1) from answer where question_id=q.id) as answers,        " +
                "       (select count(1) from grade where question_id=q.id and type=1) as likes,\n" +
                "       (select count(1) from grade where question_id=q.id and type=2) as disLikes\n" +
                "        from question q,category c,content con,user u where q.user_id=u.id and  q.category_id=c.id and q.content_id=con.id and q.title  like '%"+searchStr+"%' order by q.id desc";
        Query query=entityManager.createNativeQuery(baseQuery,QuestionModel.class);
        List<QuestionModel> questions=query.getResultList();

        model.addAttribute("questions",questions);
        model.addAttribute("categories",categoryRepository.findAll());

        return "question/questions";
    }

    @GetMapping("/question/category/{catId}/{searchStr}/search")
    public String searchByCategory(@PathVariable(value = "searchStr",required = false) String searchStr,@PathVariable(value = "catId",required = false) String catId,ModelMap model){

        if(searchStr.equals(" ")){
            searchStr="";
        }
        String baseQuery="select q.id,u.username as user,q.title,q.date,c.name as category,con.text as content,\n" +
                "       (select count(1) from answer where question_id=q.id) as answers,\n" +
                "       (select count(1) from grade where question_id=q.id and type=1) as likes,\n" +
                "       (select count(1) from grade where question_id=q.id and type=2) as disLikes\n" +
                "        from question q,category c,content con,user u  where q.user_id=u.id and  q.category_id=c.id and q.content_id=con.id and q.category_id="+catId+" and  q.title  like '%"+searchStr+"%' order by q.date desc";
        Query query=entityManager.createNativeQuery(baseQuery,QuestionModel.class);
        List<QuestionModel> questions=query.getResultList();

        model.addAttribute("questions",questions);
        model.addAttribute("categories",categoryRepository.findAll());

        return "question/questions";
    }

    @PostMapping("/api/question/like")
    @ResponseBody
    public String setLike(Long questionId){

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user=userRepository.findUserByUsername(auth.getName());
        Question question=questionRepository.getOne(questionId);
        Grade like=gradeRepository.findByUserIdAndQuestionId(user.getId(),questionId);
        if(like==null){

            Grade likes=new Grade();
            likes.setType(1);
            likes.setQuestion(question);
            likes.setUser(user);
            gradeRepository.save(likes);
        }
        else{
            like.setType(1);
            gradeRepository.save(like);
        }
        Set<Grade> likes=gradeRepository.findByTypeAndQuestion(1,question);
        Set<Grade> disLikes=gradeRepository.findByTypeAndQuestion(2,question);

        return likes.size()+"="+disLikes.size();
    }

    // var userId=[[${@userService.findUserByUsername(#authentication.name).getId()}]];

    @PostMapping("/api/question/dislike")
    @ResponseBody
    public String setDisLike(Long questionId){

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user=userRepository.findUserByUsername(auth.getName());
        Question question=questionRepository.getOne(questionId);
        Grade disLike=gradeRepository.findByUserIdAndQuestionId(user.getId(),questionId);
        if(disLike==null){
            Grade disLikes=new Grade();
            disLikes.setType(2);
            disLikes.setQuestion(question);
            disLikes.setUser(user);
            gradeRepository.save(disLikes);
        }
        else{
            disLike.setType(2);
            gradeRepository.save(disLike);
        }
        Set<Grade> likes=gradeRepository.findByTypeAndQuestion(1,question);
        Set<Grade> disLikes=gradeRepository.findByTypeAndQuestion(2,question);

        return likes.size()+"="+disLikes.size();
    }

    @GetMapping("/question/most/viewed")
    public String getMostVieweds(ModelMap model){

        String baseQuery="select q.id,q.views,u.username as user,q.title,q.date,c.name as category,con.text as content,\n" +
                "       (select count(1) from answer where question_id=q.id) as answers,               (select count(1) from grade where question_id=q.id and type=1) as likes,\n" +
                "       (select count(1) from grade where question_id=q.id and type=2) as disLikes\n" +
                "from question q,category c,content con,user u where q.user_id=u.id and  q.category_id=c.id and q.content_id=con.id order by q.views desc";

        Query query=entityManager.createNativeQuery(baseQuery,QuestionModel.class);
        List<QuestionModel> questions=query.getResultList();

        model.addAttribute("questions",questions);
        return "question/mostviewed";
    }
}
