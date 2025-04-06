import { useParams } from "react-router-dom";

const Profile = () => {
  const { username } = useParams();
  return (
    <section className="space-y-4">
      <h2 className="text-2xl font-semibold">Profile</h2>
      <p className="text-slate-700">
        Viewing profile for <span className="font-medium">{username}</span>.
      </p>
    </section>
  );
};

export default Profile;
