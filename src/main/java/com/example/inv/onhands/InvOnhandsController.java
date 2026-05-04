package com.example.inv.onhands;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@RestController
@RequestMapping("/inv/onhands")
public class InvOnhandsController {

    public ModelAndView list(InvOnhandsDTO dto) {
        ModelAndView mv = new ModelAndView("inv/onhands/list");
        mv.addObject("dto", dto);
        return mv;
    }
}
