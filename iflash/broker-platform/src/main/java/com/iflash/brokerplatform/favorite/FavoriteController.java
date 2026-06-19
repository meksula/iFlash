package com.iflash.brokerplatform.favorite;

import com.iflash.brokerplatform.user.UserSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/favorites")
class FavoriteController {

    private final FavoriteService favoriteService;
    private final UserSession userSession;

    FavoriteController(FavoriteService favoriteService, UserSession userSession) {
        this.favoriteService = favoriteService;
        this.userSession = userSession;
    }

    @PostMapping("/{ticker}")
    String add(@PathVariable String ticker, @RequestParam(defaultValue = "/instruments") String from,
               HttpSession session) {
        favoriteService.add(userSession.currentUserId(session), ticker.toUpperCase());
        return redirect(from);
    }

    @PostMapping("/{ticker}/remove")
    String remove(@PathVariable String ticker, @RequestParam(defaultValue = "/instruments") String from,
                  HttpSession session) {
        favoriteService.remove(userSession.currentUserId(session), ticker.toUpperCase());
        return redirect(from);
    }

    /** Only allow same-site relative redirects. */
    private String redirect(String from) {
        return "redirect:" + (from.startsWith("/") ? from : "/instruments");
    }
}
