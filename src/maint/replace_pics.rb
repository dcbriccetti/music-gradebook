# Input lines look like:
# "100002898","100002898.jpg"
# "100002898","100002898(2).jpg"

open('/Users/daveb/Documents/Stanley Music/2019–20/photos/IDLink.txt') do |file|
  file.each_line do |line|
    m = line.match(/"(.+)","(.+)"/)
    id, name = m[1], m[2]
    if name['(2)']
      puts "mv #{id}.jpg replaced"
      puts "mv #{id}\\(2\\).jpg #{id}.jpg"
    end
  end
end
