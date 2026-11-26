package com.zoopass.support;

import com.zoopass.notification.EmailService;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Test double: keeps sent emails in memory so tests can read verification / reset links. */
public class CapturingEmailService implements EmailService {

    public record Sent(String to, String subject, String body) {
    }

    private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+)");

    private final List<Sent> sent = new CopyOnWriteArrayList<>();

    @Override
    public void send(String to, String subject, String body) {
        sent.add(new Sent(to, subject, body));
    }

    public List<Sent> to(String email) {
        return sent.stream().filter(mail -> mail.to().equalsIgnoreCase(email)).toList();
    }

    /** Token from the latest email sent to this address. */
    public Optional<String> latestToken(String email) {
        List<Sent> mails = to(email);
        if (mails.isEmpty()) {
            return Optional.empty();
        }
        Matcher matcher = TOKEN.matcher(mails.get(mails.size() - 1).body());
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }
}
