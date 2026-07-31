-- Sliding window rate limiter (atomico).
-- KEYS[1] = chiave Redis per tenant
-- ARGV[1] = window ms, ARGV[2] = max requests, ARGV[3] = now ms, ARGV[4] = member unico
-- Ritorna {allowed (0|1), remaining}
local key = KEYS[1]
local window = tonumber(ARGV[1])
local limit = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local member = ARGV[4]

redis.call('ZREMRANGEBYSCORE', key, 0, now - window)
local count = redis.call('ZCARD', key)

if count < limit then
  redis.call('ZADD', key, now, member)
  redis.call('PEXPIRE', key, window)
  return {1, limit - count - 1}
end

return {0, 0}
