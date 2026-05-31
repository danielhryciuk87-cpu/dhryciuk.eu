<?php
declare(strict_types=1);

$to = 'daniel@dhryciuk.eu';
$name = trim($_POST['name'] ?? '');
$email = trim($_POST['email'] ?? '');
$message = trim($_POST['message'] ?? '');

if ($name === '' || $message === '' || !filter_var($email, FILTER_VALIDATE_EMAIL)) {
    http_response_code(422);
    echo 'Uzupełnij poprawnie formularz.';
    exit;
}

$subject = 'Nowa wiadomość z dhryciuk.eu';
$body = "Imię i nazwisko: {$name}\nE-mail: {$email}\n\nWiadomość:\n{$message}\n";
$headers = [
    'From: formularz@dhryciuk.eu',
    'Reply-To: ' . $email,
    'Content-Type: text/plain; charset=UTF-8',
];

if (mail($to, $subject, $body, implode("\r\n", $headers))) {
    header('Location: /contact.html?sent=1');
    exit;
}

http_response_code(500);
echo 'Nie udało się wysłać wiadomości.';
