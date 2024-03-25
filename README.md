# Nginx Log Hub

This is a hub for nginx access logs. Concentrates logs entries to a single file by day to facilitate log backup or post
process. Ideal for personal sites or small projects.

## Nginx configuration

Add this log format to your nginx configuration file:

```nginx
log_format loghub '$host $remote_addr - $remote_user [$time_local] '
                      '"$request" $status $body_bytes_sent '
                      '"$http_referer" "$http_user_agent" '
                      '$upstream_response_time $request_time';
```

Then in your sites configuration file, add the following line to the `server` block:

```nginx
access_log tcp://localhost:8050 loghub;
```

## Features

- [ ] status page
- [ ] stadistics page
- [ ] log entries search
